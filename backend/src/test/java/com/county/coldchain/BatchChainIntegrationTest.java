package com.county.coldchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 批次链全链路集成测试（H2）：
 * 到货（温度超标+混批混装）→ 质检转分拣 → 分拣（损耗争议）→ 催货/锁价
 * → 温区故障 → 转仓 → 报损 → 优先出库（临时改客户）→ 坏果投诉 → 账单结算；
 * 另测按原重结算与整车退货批次。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BatchChainIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    private String token;

    @BeforeEach
    void login() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}"""))
                .andExpect(status().isOk()).andReturn();
        token = om.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("token").asText();
    }

    private JsonNode apiPost(String path, String json) throws Exception {
        MvcResult r = mvc.perform(post(path).header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JsonNode apiPut(String path, String json) throws Exception {
        MvcResult r = mvc.perform(put(path).header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JsonNode apiGet(String path) throws Exception {
        MvcResult r = mvc.perform(get(path).header("X-Auth-Token", token))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void fullChain_registerQcSortTransferLossOutboundComplaintSettle() throws Exception {
        // 1. 到货：草莓 1000kg，到货温度 9℃（限值 6℃，超标），混批混装
        JsonNode batch = apiPost("/api/batches/register", """
                {"vehicleNo":"皖A·12345","ownerId":2,"categoryId":2,"origin":"长丰草莓基地",
                 "arrivalWeight":1000,"arrivalTemp":9,"packaging":"泡沫箱","quarantineNo":"JY2026100401",
                 "plannedCustomerId":2,"settleMethod":"ACTUAL","mixedLoad":true}""");
        Long batchId = batch.get("id").asLong();
        assertEquals("待质检", batch.get("status").get("label").asText());

        // 2. 质检：转分拣，接受 980kg（20kg 退回）
        JsonNode qc = apiPost("/api/qc", """
                {"batchId":%d,"appearanceScore":4,"maturityScore":4,"rotRate":3.5,"moistureRate":88,
                 "measuredTemp":7,"documentsOk":true,"grade":"B","result":"SORT","acceptedWeight":980,
                 "remark":"到货温度超标，先分拣分级"}""".formatted(batchId));
        assertEquals("SORT", qc.get("result").get("code").asText());
        assertTrue(qc.get("tempExceeded").asBoolean());
        assertTrue(qc.get("acceptedWeight").decimalValue().compareTo(new BigDecimal("980")) == 0);
        assertTrue(qc.get("returnWeight").decimalValue().compareTo(new BigDecimal("20")) == 0);

        // 3. 分拣：A 300 / B 500 / C 130，损耗 50kg，标记损耗争议
        JsonNode sort = apiPost("/api/sort", """
                {"batchId":%d,"inputWeight":980,"weightA":300,"weightB":500,"weightC":130,
                 "laborHours":12,"laborRate":25,"lossResponsible":"OWNER","disputed":true,
                 "disputeNote":"货主认为腐损主要由冷库延迟入库导致，待复核"}""".formatted(batchId));
        assertTrue(sort.get("lossWeight").decimalValue().compareTo(new BigDecimal("50")) == 0);
        assertTrue(sort.get("disputed").asBoolean());

        JsonNode detail = apiGet("/api/batches/" + batchId);
        assertEquals("在仓", detail.get("status").get("label").asText());
        assertTrue(detail.get("saleableWeight").decimalValue().compareTo(new BigDecimal("930")) == 0);
        // A 级 300kg 落冰温库
        JsonNode lots = detail.get("lots");
        assertTrue(lots.size() >= 3);

        // 4. 客户催货（惠民超市，优先）
        apiPost("/api/demands", """
                {"batchId":%d,"customerId":2,"priority":true,"note":"门店促销等货"}""".formatted(batchId));

        // 5. 冰温库故障 → 批次链出现 ZONE_FAULT 事件
        apiPut("/api/zones/3/fault", """
                {"faulty":true,"note":"压缩机报警"}""");
        JsonNode traceFault = apiGet("/api/batches/" + batchId + "/trace");
        boolean hasFaultEvent = false;
        for (JsonNode e : traceFault) if ("ZONE_FAULT".equals(e.get("type").get("code").asText())) hasFaultEvent = true;
        assertTrue(hasFaultEvent, "温区故障必须写入批次链");

        // 6. 转仓到 2 号冷藏库
        JsonNode moved = apiPost("/api/transfer", """
                {"batchId":%d,"toZoneId":2,"reason":"冰温库压缩机故障，紧急转仓"}""".formatted(batchId));
        assertTrue(moved.size() >= 1);

        // 7. 报损 30kg B 级（温区故障期间坏果，冷库承担责任）
        Long bLotId = null;
        for (JsonNode l : apiGet("/api/batches/" + batchId).get("lots")) {
            if ("IN_ZONE".equals(l.get("status").get("code").asText()) && "B".equals(l.get("grade").get("code").asText())) {
                bLotId = l.get("id").asLong();
            }
        }
        assertNotNull(bLotId);
        apiPost("/api/loss", """
                {"batchId":%d,"lotId":%d,"grade":"B","weight":30,"reason":"转仓期间升温坏果","responsible":"COLD_STORAGE"}"""
                .formatted(batchId, bLotId));

        // 8. 临时改客户后优先出库 A 级 300kg（城东鲜果连锁，其 A 级草莓锁价 28 元）
        apiPost("/api/customer-change", """
                {"batchId":%d,"customerId":1,"reason":"原客户促销延期，转给城东鲜果锁价单"}""".formatted(batchId));
        JsonNode out = apiPost("/api/outbound", """
                {"batchId":%d,"customerId":1,"grade":"A","weight":300,"channel":"NORMAL",
                 "priority":true,"customerChanged":true,"remark":"优先满足锁价客户"}""".formatted(batchId));
        assertTrue(out.get("unitPrice").decimalValue().compareTo(new BigDecimal("28.00")) == 0, "应采用锁价28元");
        assertTrue(out.get("amount").decimalValue().compareTo(new BigDecimal("8400.00")) == 0);
        assertTrue(out.get("customerChanged").asBoolean());
        Long outId = out.get("id").asLong();

        // 9. 出库后客户投诉坏果 20kg
        JsonNode comp = apiPost("/api/complaints", """
                {"outboundId":%d,"content":"开箱发现下层压伤坏果","badWeight":20,"responsible":"OWNER"}"""
                .formatted(outId));
        assertTrue(comp.get("compensation").decimalValue().compareTo(new BigDecimal("560.00")) == 0);

        // 10. 剩余 B/C 转加工渠道出清
        apiPost("/api/outbound", """
                {"batchId":%d,"customerId":3,"grade":"B","weight":470,"channel":"PROCESSING"}""".formatted(batchId));
        apiPost("/api/outbound", """
                {"batchId":%d,"customerId":3,"grade":"C","weight":130,"channel":"PROCESSING"}""".formatted(batchId));

        detail = apiGet("/api/batches/" + batchId);
        assertEquals("已出清", detail.get("status").get("label").asText());
        assertTrue(detail.get("shippedWeight").decimalValue().compareTo(new BigDecimal("900")) == 0);
        assertTrue(detail.get("lossWeight").decimalValue().compareTo(new BigDecimal("80")) == 0);
        assertTrue(detail.get("finalDestination").asText().contains("城东鲜果连锁（批发客户）"));
        assertTrue(detail.get("finalDestination").asText().contains("鲜味加工厂（加工渠道）"));

        // 11. 生成账单并结算
        JsonNode bill = apiPost("/api/bills/generate", "{\"batchId\":" + batchId + "}");
        // 货值 = 8400 + 470*22 + 130*14
        assertTrue(bill.get("goodsAmount").decimalValue().compareTo(new BigDecimal("20560.00")) == 0);
        // 投诉赔付 560；冷库责任的 30kg 报损不扣货主；货主预付款 5000 抵扣
        assertTrue(bill.get("complaintDeduction").decimalValue().compareTo(new BigDecimal("560.00")) == 0);
        assertTrue(bill.get("storageFee").decimalValue().signum() > 0);
        assertTrue(bill.get("sortFee").decimalValue().signum() > 0);
        assertTrue(bill.get("advanceDeduction").decimalValue().signum() > 0);
        Long billId = bill.get("id").asLong();

        JsonNode settled = apiPost("/api/bills/" + billId + "/settle", "{}");
        assertEquals("已结算", settled.get("status").get("label").asText());
        assertEquals("已结算关账", apiGet("/api/batches/" + batchId).get("status").get("label").asText());

        // 12. 批次链完整且有序
        JsonNode events = apiGet("/api/batches/" + batchId + "/trace");
        String[] expect = {"REGISTER", "QC", "SORT", "DEMAND", "ZONE_FAULT", "MOVE", "LOSS",
                "CUSTOMER_CHANGE", "OUTBOUND", "COMPLAINT", "OUTBOUND", "OUTBOUND", "BILL", "SETTLE"};
        int i = 0;
        for (JsonNode e : events) {
            assertEquals(expect[i], e.get("type").get("code").asText(), "事件序" + i);
            assertEquals(i + 1, e.get("seq").asInt());
            i++;
        }
        assertEquals(expect.length, i);

        // 13. 看板：该批已关账不列入在仓，但高风险/汇总可正常返回
        JsonNode dash = apiGet("/api/dashboard");
        assertTrue(dash.get("totalBatches").asInt() >= 1);
        assertTrue(dash.get("zones").size() >= 4);
    }

    @Test
    void settleByOriginalWeight_deductsReturnAndOwnerLoss() throws Exception {
        // 葡萄按到货原重结算；质检降级 B 级直接入库
        JsonNode batch = apiPost("/api/batches/register", """
                {"vehicleNo":"皖B·666","ownerId":1,"categoryId":1,"origin":"大圩葡萄镇",
                 "arrivalWeight":2000,"arrivalTemp":7,"packaging":"塑料筐","quarantineNo":"JY-PT-02",
                 "plannedCustomerId":1,"settleMethod":"ORIGINAL","mixedLoad":false}""");
        Long id = batch.get("id").asLong();
        apiPost("/api/qc", """
                {"batchId":%d,"appearanceScore":3,"maturityScore":4,"rotRate":6,"moistureRate":82,
                 "measuredTemp":7,"documentsOk":true,"grade":"B","result":"DEGRADE","acceptedWeight":1900,
                 "remark":"掉粒偏多降级"}""".formatted(id));
        // 报损货主责任 100kg
        JsonNode d = apiGet("/api/batches/" + id);
        long lotId = d.get("lots").get(0).get("id").asLong();
        apiPost("/api/loss", """
                {"batchId":%d,"lotId":%d,"grade":"B","weight":100,"reason":"自然腐损","responsible":"OWNER"}"""
                .formatted(id, lotId));

        JsonNode bill = apiPost("/api/bills/generate", "{\"batchId\":" + id + "}");
        assertEquals("ORIGINAL", bill.get("settleMethod").get("code").asText());
        assertTrue(bill.get("settleWeight").decimalValue().compareTo(new BigDecimal("2000")) == 0);
        // 货值 = 2000 * B级9元 = 18000
        assertTrue(bill.get("goodsAmount").decimalValue().compareTo(new BigDecimal("18000.00")) == 0);
        // 退货扣款 100kg * 9 = 900；货主损耗 100*9 = 900
        assertTrue(bill.get("returnDeduction").decimalValue().compareTo(new BigDecimal("900.00")) == 0);
        assertTrue(bill.get("lossDeduction").decimalValue().compareTo(new BigDecimal("900.00")) == 0);
        // 合作社预付款 20000，但应付为正的部分才抵扣，且不超过应付
        assertTrue(bill.get("advanceDeduction").decimalValue().signum() > 0);
    }

    @Test
    void fullReturnBatch_andGuards() throws Exception {
        JsonNode batch = apiPost("/api/batches/register", """
                {"vehicleNo":"皖C·001","ownerId":3,"categoryId":6,"origin":"巢湖水产",
                 "arrivalWeight":500,"arrivalTemp":3,"packaging":"散冰","quarantineNo":"MISSING",
                 "plannedCustomerId":3,"settleMethod":"ACTUAL","mixedLoad":false}""");
        Long id = batch.get("id").asLong();
        JsonNode qc = apiPost("/api/qc", """
                {"batchId":%d,"appearanceScore":1,"maturityScore":2,"rotRate":30,"moistureRate":90,
                 "measuredTemp":3,"documentsOk":false,"grade":"C","result":"RETURN","remark":"票证缺失整车退回"}"""
                .formatted(id));
        assertTrue(qc.get("returnWeight").decimalValue().compareTo(new BigDecimal("500")) == 0);
        assertEquals("已退货", apiGet("/api/batches/" + id).get("status").get("label").asText());

        // 已退货批次不能出库
        mvc.perform(post("/api/outbound").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"batchId":%d,"customerId":3,"grade":"C","weight":10,"channel":"NORMAL"}""".formatted(id)))
                .andExpect(status().is4xxClientError());

        // 未登录 401
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }
}
