<template>
  <div class="grid2" style="grid-template-columns: 360px 1fr">
    <div class="panel" style="margin:0">
      <h3>批次列表 <button class="ghost mini" style="float:right" @click="loadList">刷新</button></h3>
      <input v-model="kw" placeholder="搜索批次号/货主/品类/车牌" style="margin-bottom:10px" />
      <div class="batch-list">
        <div v-for="b in filtered" :key="b.id" class="batch-item"
             :class="{on: ui.batchId===b.id, risk: riskOf(b)}" @click="select(b.id)">
          <div><b>{{ b.batchNo }}</b>
            <span class="tag" :class="statusColor(b.status?.code)" style="float:right">{{ lbl(b.status) }}</span>
          </div>
          <div class="muted">{{ b.category?.name }} · {{ b.owner?.name }} · {{ b.arrivalWeight }}kg</div>
          <div>
            <span v-if="tempOf(b)" class="tag red">温度超标</span>
            <span v-if="b.mixedLoad" class="tag amber">混批混装</span>
            <span class="muted" style="font-size:12px">{{ b.vehicleNo }}</span>
          </div>
        </div>
        <div v-if="!filtered.length" class="muted">无批次</div>
      </div>
    </div>

    <div v-if="!d.id" class="panel muted">请从左侧选择一个批次进行质检、分拣、库位、出库、追溯与结算作业。</div>

    <div v-else>
      <div class="panel">
        <h3>
          {{ d.batchNo }}
          <span class="tag" :class="statusColor(d.status?.code)">{{ lbl(d.status) }}</span>
          <span v-if="d.tempExceeded" class="tag red">到货温度 {{ d.arrivalTemp }}℃ 超标（限值 {{ d.category.maxArrivalTemp }}℃）</span>
          <span v-if="d.mixedLoad" class="tag amber">混批混装</span>
        </h3>
        <div class="kv">
          <div class="k">货主</div><div>{{ d.owner.name }}（{{ lbl(d.owner.type) }}）预付款余额 {{ d.ownerAdvanceBalance }} 元</div>
          <div class="k">品类/产地</div><div>{{ d.category.name }} / {{ d.origin }}，车牌 {{ d.vehicleNo }}</div>
          <div class="k">重量链</div>
          <div>到货原重 <b>{{ fmt(d.arrivalWeight) }}</b>kg →
            在仓可售 <b style="color:var(--ok)">{{ fmt(d.saleableWeight) }}</b>kg ｜
            已出 <b>{{ fmt(d.shippedWeight) }}</b>kg ｜
            损耗 <b style="color:var(--danger)">{{ fmt(d.lossWeight) }}</b>kg ｜
            退货 {{ fmt(d.returnedWeight) }}kg ｜
            投诉坏果 {{ fmt(d.complaintDeductWeight) }}kg
          </div>
          <div class="k">包装/票证</div><div>{{ d.packaging }} / 检疫证 {{ d.quarantineNo }}</div>
          <div class="k">计划客户</div><div>{{ d.plannedCustomer?.name || '未指定' }}　结算方式：{{ lbl(d.settleMethod) }}</div>
          <div class="k">最终流向</div><div>{{ d.finalDestination || '尚未出库' }}</div>
        </div>

        <div class="actions" style="margin-top:14px">
          <button v-if="d.status.code==='REGISTERED'" @click="show='qc'">质检验收</button>
          <button v-if="canSort" @click="show='sort'">分拣</button>
          <button v-if="inStore" @click="show='loss'">报损</button>
          <button v-if="inStore" @click="show='out'">出库</button>
          <button v-if="inStore" @click="show='transfer'">转仓</button>
          <button @click="show='demand'">客户催货</button>
          <button @click="show='change'">临时改客户</button>
          <button v-if="d.outbounds.length" @click="show='complaint'">出库后投诉</button>
          <button class="ghost" @click="genBill">生成账单</button>
        </div>
      </div>

      <div class="grid2">
        <div class="panel" style="margin:0">
          <h3>库位与温区（分拣结果落位）</h3>
          <table>
            <thead><tr><th>库位</th><th>温区</th><th>等级</th><th>重量kg</th><th>起算日</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="l in d.lots" :key="l.id">
                <td>{{ l.locationCode }}</td>
                <td>{{ l.zone.name }}<span v-if="l.zone.faulty" class="tag red">故障</span></td>
                <td>{{ lbl(l.grade) }}</td>
                <td class="num">{{ fmt(l.weight) }}</td>
                <td>{{ l.storageFrom }}</td>
                <td><span class="tag" :class="l.status==='IN_ZONE'?'blue':''">{{ lotStatus(l.status) }}</span></td>
              </tr>
            </tbody>
          </table>

          <h3 style="margin-top:18px">出库记录</h3>
          <table>
            <thead><tr><th>时间</th><th>客户</th><th>等级</th><th>重量</th><th>单价</th><th>金额</th><th>渠道/标记</th></tr></thead>
            <tbody>
              <tr v-for="o in d.outbounds" :key="o.id">
                <td>{{ fmtTime(o.outboundAt) }}</td><td>{{ o.customer.name }}</td>
                <td>{{ lbl(o.grade) }}</td><td class="num">{{ fmt(o.weight) }}</td>
                <td class="num">{{ o.unitPrice }}</td><td class="num">{{ fmt(o.amount) }}</td>
                <td>
                  {{ lbl(o.channel) }}
                  <span v-if="o.priority" class="tag red">优先</span>
                  <span v-if="o.customerChanged" class="tag amber">改客户</span>
                  <span v-if="o.priceLock" class="tag green">锁价</span>
                </td>
              </tr>
              <tr v-if="!d.outbounds.length"><td colspan="7" class="muted">暂无出库</td></tr>
            </tbody>
          </table>

          <h3 style="margin-top:18px">分拣 / 损耗 / 投诉</h3>
          <table>
            <tbody>
              <tr v-for="s in d.sorts" :key="s.id">
                <td>分拣：投线{{ fmt(s.inputWeight) }}kg → A{{ s.weightA }}/B{{ s.weightB }}/C{{ s.weightC }}，
                  损耗<b style="color:var(--danger)">{{ fmt(s.lossWeight) }}</b>kg（{{ lbl(s.lossResponsible) }}责任）
                  工时{{ s.laborHours }}×{{ s.laborRate }}元
                  <span v-if="s.disputed" class="tag red">⚠损耗争议：{{ s.disputeNote }}</span>
                </td>
              </tr>
              <tr v-for="l in d.losses" :key="l.id">
                <td>报损：{{ lbl(l.grade) }} {{ fmt(l.weight) }}kg，{{ l.reason }}，
                  <span class="tag" :class="l.responsible.code==='COLD_STORAGE'?'red':'amber'">{{ lbl(l.responsible) }}承担</span>
                </td>
              </tr>
              <tr v-for="c in d.complaints" :key="c.id">
                <td>投诉坏果 {{ fmt(c.badWeight) }}kg，赔付 <b>{{ fmt(c.compensation) }}</b> 元（{{ lbl(c.responsible) }}责任）：{{ c.content }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="panel" style="margin:0">
          <h3>批次链（监管追溯：入库→最终客户）</h3>
          <div class="timeline">
            <div v-for="e in d.events" :key="e.id" class="tl-item">
              <div>
                <span class="tag" :class="eventColor(e.type.code)">{{ eventName(e.type.code) }}</span>
                <span class="t">#{{ e.seq }} {{ fmtTime(e.occurredAt) }} · {{ e.operator }}</span>
              </div>
              <div class="d">{{ e.detail }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 质检弹窗 -->
      <Modal v-if="show==='qc'" title="质检验收（六维判定）" @close="show=''">
        <div class="row">
          <div class="field"><label>外观(1-5)</label><input v-model.number="qc.appearanceScore" type="number" min="1" max="5" /></div>
          <div class="field"><label>成熟度(1-5)</label><input v-model.number="qc.maturityScore" type="number" min="1" max="5" /></div>
          <div class="field"><label>腐损率%</label><input v-model.number="qc.rotRate" type="number" step="0.1" /></div>
        </div>
        <div class="row">
          <div class="field"><label>含水率%</label><input v-model.number="qc.moistureRate" type="number" step="0.1" /></div>
          <div class="field"><label>实测温度℃</label><input v-model.number="qc.measuredTemp" type="number" step="0.1" /></div>
          <div class="field"><label>票证齐全</label>
            <select v-model="qc.documentsOk"><option :value="true">齐全</option><option :value="false">缺失</option></select>
          </div>
        </div>
        <div class="row">
          <div class="field"><label>验收结论</label>
            <select v-model="qc.result">
              <option value="INBOUND">入库</option>
              <option value="DEGRADE">降级入库</option>
              <option value="SORT">转分拣</option>
              <option value="RETURN">退货</option>
            </select>
          </div>
          <div class="field"><label>定级</label>
            <select v-model="qc.grade"><option>A</option><option>B</option><option>C</option></select>
          </div>
          <div class="field"><label>接受重量kg（其余按退货）</label><input v-model.number="qc.acceptedWeight" type="number" step="0.01" /></div>
        </div>
        <div class="field"><label>备注（超标/混装/降级说明）</label><textarea v-model="qc.remark" rows="2"></textarea></div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button @click="doQc">提交质检</button>
        </template>
      </Modal>

      <!-- 分拣弹窗 -->
      <Modal v-if="show==='sort'" title="分拣（分级产出、损耗、工时）" @close="show=''">
        <div class="row">
          <div class="field"><label>投线重量kg</label><input v-model.number="sort.inputWeight" type="number" step="0.01" /></div>
          <div class="field"><label>A级产出kg</label><input v-model.number="sort.weightA" type="number" step="0.01" /></div>
          <div class="field"><label>B级产出kg</label><input v-model.number="sort.weightB" type="number" step="0.01" /></div>
          <div class="field"><label>C级产出kg</label><input v-model.number="sort.weightC" type="number" step="0.01" /></div>
        </div>
        <div class="row">
          <div class="field"><label>分拣工时</label><input v-model.number="sort.laborHours" type="number" step="0.5" /></div>
          <div class="field"><label>工时单价元</label><input v-model.number="sort.laborRate" type="number" step="1" /></div>
          <div class="field"><label>损耗责任</label>
            <select v-model="sort.lossResponsible">
              <option value="OWNER">货主</option><option value="COLD_STORAGE">冷库</option><option value="CARRIER">承运方</option>
            </select>
          </div>
        </div>
        <div class="field">
          <label><input type="checkbox" style="width:auto" v-model="sort.disputed" /> 分拣损耗有争议（货主不认可损耗量/责任）</label>
          <textarea v-if="sort.disputed" v-model="sort.disputeNote" rows="2" placeholder="争议说明"></textarea>
        </div>
        <div class="muted">本次分拣损耗 = 投线 - (A+B+C) = <b>{{ sortLoss }}kg</b>；产出将自动分配到对应保鲜温区并更新可售重量与售价等级。</div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button @click="doSort">提交分拣</button>
        </template>
      </Modal>

      <!-- 报损 -->
      <Modal v-if="show==='loss'" title="在仓报损（减少可售重量、认定损耗责任）" @close="show=''">
        <div class="row">
          <div class="field"><label>库位</label>
            <select v-model="loss.lotId">
              <option v-for="l in openLots" :key="l.id" :value="l.id">
                {{ l.locationCode }} {{ l.zone.name }} {{ lbl(l.grade) }} 现存{{ fmt(l.weight) }}kg
              </option>
            </select>
          </div>
          <div class="field"><label>等级</label><select v-model="loss.grade"><option>A</option><option>B</option><option>C</option></select></div>
          <div class="field"><label>报损重量kg</label><input v-model.number="loss.weight" type="number" step="0.01" /></div>
        </div>
        <div class="row">
          <div class="field"><label>原因</label><input v-model="loss.reason" placeholder="坏果/温区故障/超期/温度超标" /></div>
          <div class="field"><label>责任方</label>
            <select v-model="loss.responsible">
              <option value="OWNER">货主</option><option value="COLD_STORAGE">冷库</option><option value="CARRIER">承运方</option>
            </select>
          </div>
        </div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button class="danger" @click="doLoss">确认报损</button>
        </template>
      </Modal>

      <!-- 出库 -->
      <Modal v-if="show==='out'" title="出库（锁价优先 / 改客户 / 加工渠道）" @close="show=''">
        <div class="row">
          <div class="field"><label>客户</label>
            <select v-model="out.customerId">
              <option v-for="c in meta.customers" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="field"><label>等级</label>
            <select v-model="out.grade" @change="fillAvail">
              <option>A</option><option>B</option><option>C</option>
            </select>
          </div>
          <div class="field"><label>出库重量kg（在库{{ availByGrade[out.grade]||0 }}kg）</label>
            <input v-model.number="out.weight" type="number" step="0.01" />
          </div>
        </div>
        <div class="row">
          <div class="field"><label>渠道</label>
            <select v-model="out.channel"><option value="NORMAL">批发客户</option><option value="PROCESSING">加工渠道</option></select>
          </div>
          <div class="field"><label><input type="checkbox" style="width:auto" v-model="out.priority" /> 客户要求优先出库</label></div>
          <div class="field"><label><input type="checkbox" style="width:auto" v-model="out.customerChanged" /> 本次临时改客户</label></div>
        </div>
        <div class="muted">价格优先级：指定锁价单 ＞ 客户有效锁价 ＞ 品类等级参考价（{{ gradePrices }}）。FIFO 按最早入库库位扣减。</div>
        <div class="field"><label>备注</label><input v-model="out.remark" /></div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button @click="doOutbound">确认出库</button>
        </template>
      </Modal>

      <!-- 转仓 -->
      <Modal v-if="show==='transfer'" title="转仓（温区故障/调度，新库位仓储费重新起算）" @close="show=''">
        <div class="row">
          <div class="field"><label>目标温区</label>
            <select v-model="transfer.toZoneId">
              <option v-for="z in meta.zones" :key="z.id" :value="z.id" :disabled="z.faulty">
                {{ z.name }}（{{ z.zoneType }}，{{ z.faulty ? '故障中' : '可用' }}）
              </option>
            </select>
          </div>
        </div>
        <div class="field"><label>原因</label><input v-model="transfer.reason" placeholder="如：冰温库压缩机故障，紧急转仓" /></div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button @click="doTransfer">确认转仓</button>
        </template>
      </Modal>

      <!-- 催货 -->
      <Modal v-if="show==='demand'" title="客户催货" @close="show=''">
        <div class="row">
          <div class="field"><label>客户</label>
            <select v-model="demand.customerId">
              <option v-for="c in meta.customers" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>
        <div class="field"><label><input type="checkbox" style="width:auto" v-model="demand.priority" /> 要求优先出库</label></div>
        <div class="field"><label>说明</label><input v-model="demand.note" /></div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button @click="doDemand">登记催货</button>
        </template>
      </Modal>

      <!-- 改客户 -->
      <Modal v-if="show==='change'" title="临时改客户" @close="show=''">
        <div class="row">
          <div class="field"><label>新客户</label>
            <select v-model="change.customerId">
              <option v-for="c in meta.customers" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>
        <div class="field"><label>原因</label><input v-model="change.reason" /></div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button @click="doChange">确认改客户</button>
        </template>
      </Modal>

      <!-- 投诉 -->
      <Modal v-if="show==='complaint'" title="出库后客户投诉坏果" @close="show=''">
        <div class="row">
          <div class="field"><label>出库单</label>
            <select v-model="complaint.outboundId">
              <option v-for="o in d.outbounds" :key="o.id" :value="o.id">
                {{ fmtTime(o.outboundAt) }} {{ o.customer.name }} {{ lbl(o.grade) }} {{ o.weight }}kg @{{ o.unitPrice }}
              </option>
            </select>
          </div>
          <div class="field"><label>坏果重量kg</label><input v-model.number="complaint.badWeight" type="number" step="0.01" /></div>
          <div class="field"><label>赔付单价(留空取出库价)</label><input v-model.number="complaint.unitPrice" type="number" step="0.01" /></div>
          <div class="field"><label>责任方</label>
            <select v-model="complaint.responsible">
              <option value="OWNER">货主</option><option value="COLD_STORAGE">冷库</option><option value="CARRIER">承运方</option>
            </select>
          </div>
        </div>
        <div class="field"><label>投诉内容</label><textarea v-model="complaint.content" rows="2"></textarea></div>
        <template #footer>
          <button class="ghost" @click="show=''">取消</button>
          <button class="danger" @click="doComplaint">登记赔付</button>
        </template>
      </Modal>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api, lbl, toast } from '../api'
import { ui } from '../state'
import Modal from '../components/Modal.vue'

const list = ref([])
const d = ref({})
const meta = ref({ customers: [], zones: [] })
const kw = ref('')
const show = ref('')

const qc = reactive({ appearanceScore: 4, maturityScore: 4, rotRate: 0, moistureRate: 0,
  measuredTemp: null, documentsOk: true, result: 'INBOUND', grade: 'A', acceptedWeight: null, remark: '' })
const sort = reactive({ inputWeight: 0, weightA: 0, weightB: 0, weightC: 0,
  laborHours: 0, laborRate: 25, lossResponsible: 'OWNER', disputed: false, disputeNote: '' })
const loss = reactive({ lotId: null, grade: 'B', weight: 0, reason: '', responsible: 'OWNER' })
const out = reactive({ customerId: null, grade: 'A', weight: 0, channel: 'NORMAL',
  priority: false, customerChanged: false, remark: '' })
const transfer = reactive({ toZoneId: null, reason: '' })
const demand = reactive({ customerId: null, priority: false, note: '' })
const change = reactive({ customerId: null, reason: '' })
const complaint = reactive({ outboundId: null, badWeight: 0, unitPrice: null, responsible: 'OWNER', content: '' })

const inStore = computed(() => ['IN_STORAGE', 'PARTIAL_OUT'].includes(d.value.status?.code))
const canSort = computed(() => ['SORTING', 'IN_STORAGE'].includes(d.value.status?.code))
const openLots = computed(() => (d.value.lots || []).filter(l => l.status === 'IN_ZONE'))
const availByGrade = computed(() => {
  const m = { A: 0, B: 0, C: 0 }
  openLots.value.forEach(l => { m[l.grade.code] += Number(l.weight) })
  return m
})
const sortLoss = computed(() =>
  Math.max(0, Number(sort.inputWeight || 0) - Number(sort.weightA || 0) - Number(sort.weightB || 0) - Number(sort.weightC || 0)))
const gradePrices = computed(() => {
  const c = d.value.category
  return c ? `A ${c.priceA} / B ${c.priceB} / C ${c.priceC} 元/kg` : ''
})

const filtered = computed(() => {
  const k = kw.value.trim()
  if (!k) return list.value
  return list.value.filter(b =>
    [b.batchNo, b.vehicleNo, b.category?.name, b.owner?.name].some(x => (x || '').includes(k)))
})

function fmt(v) { return v == null ? '0' : Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) }
function fmtTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function lotStatus(s) { return { IN_ZONE: '在库', TRANSFERRED: '已转仓', OUT: '已出库', SCRAPPED: '已报损', PENDING_SORT: '待分拣' }[s] || s }
function statusColor(c) {
  return { IN_STORAGE: 'blue', PARTIAL_OUT: 'amber', SHIPPED: 'green', RETURNED: 'red',
    SORTING: 'purple', REGISTERED: '', CLOSED: 'green' }[c] || ''
}
function eventName(t) {
  return { REGISTER: '到货登记', QC: '质检', SORT: '分拣', MOVE: '转仓', LOSS: '报损',
    OUTBOUND: '出库', COMPLAINT: '投诉', DEMAND: '催货', ZONE_FAULT: '温区故障',
    CUSTOMER_CHANGE: '改客户', RETURN: '退货', BILL: '账单', SETTLE: '结算关账' }[t] || t
}
function eventColor(t) {
  if (['COMPLAINT', 'ZONE_FAULT', 'RETURN', 'LOSS'].includes(t)) return 'red'
  if (['OUTBOUND', 'MOVE', 'CUSTOMER_CHANGE', 'DEMAND'].includes(t)) return 'amber'
  if (['BILL', 'SETTLE'].includes(t)) return 'green'
  return 'blue'
}
function tempOf(b) { return Number(b.arrivalTemp) > Number(b.category?.maxArrivalTemp) }
function riskOf(b) { return b.mixedLoad || tempOf(b) }

async function loadList() { list.value = await api.get('/api/batches') }
async function select(id) {
  ui.batchId = id
  d.value = await api.get('/api/batches/' + id)
  if (!out.customerId) out.customerId = d.value.plannedCustomer?.id || meta.value.customers[0]?.id
}
function fillAvail() { out.weight = availByGrade.value[out.grade] || 0 }

async function reload() { await loadList(); await select(ui.batchId) }

async function doQc() {
  try { await api.post('/api/qc', { batchId: d.value.id, ...qc }); toast('质检完成', true); show.value = ''; await reload() }
  catch (e) { toast(e.message) }
}
async function doSort() {
  try { await api.post('/api/sort', { batchId: d.value.id, ...sort }); toast('分拣完成并已落库', true); show.value = ''; await reload() }
  catch (e) { toast(e.message) }
}
async function doLoss() {
  try { await api.post('/api/loss', { batchId: d.value.id, ...loss }); toast('报损已入账，可售重量已调整', true); show.value = ''; await reload() }
  catch (e) { toast(e.message) }
}
async function doOutbound() {
  try { await api.post('/api/outbound', { batchId: d.value.id, ...out }); toast('出库成功', true); show.value = ''; await reload() }
  catch (e) { toast(e.message) }
}
async function doTransfer() {
  try { await api.post('/api/transfer', { batchId: d.value.id, ...transfer }); toast('转仓完成', true); show.value = ''; await reload() }
  catch (e) { toast(e.message) }
}
async function doDemand() {
  try { await api.post('/api/demands', { batchId: d.value.id, ...demand }); toast('催货已登记，看板将提示优先出库', true); show.value = ''; await reload() }
  catch (e) { toast(e.message) }
}
async function doChange() {
  try { await api.post('/api/customer-change', { batchId: d.value.id, ...change }); toast('已临时改客户', true); show.value = ''; await reload() }
  catch (e) { toast(e.message) }
}
async function doComplaint() {
  try {
    await api.post('/api/complaints', { ...complaint,
      unitPrice: complaint.unitPrice || null })
    toast('投诉赔付已登记并写进批次链', true); show.value = ''; await reload()
  } catch (e) { toast(e.message) }
}
async function genBill() {
  try {
    await api.post('/api/bills/generate', { batchId: d.value.id })
    toast('账单已生成，请前往「结算账单」核对并结算', true)
    await reload()
  } catch (e) { toast(e.message) }
}

onMounted(async () => {
  meta.value = await api.get('/api/meta')
  await loadList()
  if (ui.batchId) await select(ui.batchId)
})
</script>

<style scoped>
.batch-list { max-height: calc(100vh - 180px); overflow: auto; }
.batch-item { padding: 9px 10px; border: 1px solid var(--line); border-radius: 8px; margin-bottom: 8px; cursor: pointer; background: #fff; }
.batch-item:hover { border-color: var(--brand); }
.batch-item.on { border-color: var(--brand); box-shadow: 0 0 0 2px rgba(31,111,84,.18); }
.batch-item.risk { border-left: 3px solid var(--danger); }
</style>
