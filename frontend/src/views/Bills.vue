<template>
  <div class="grid2" style="grid-template-columns: 1fr 1.2fr">
    <div class="panel" style="margin:0">
      <h3>账单列表 <button class="ghost mini" style="float:right" @click="load">刷新</button></h3>
      <table>
        <thead><tr><th>账单号</th><th>批次</th><th>货主</th><th>结算方式</th><th>应付货主</th><th>状态</th><th></th></tr></thead>
        <tbody>
          <tr v-for="b in bills" :key="b.id" :style="selId===b.id?'background:#f2f8f5':''">
            <td>{{ b.billNo }}</td>
            <td>{{ b.batch.batchNo }}</td>
            <td>{{ b.owner.name }}</td>
            <td>{{ lbl(b.settleMethod) }}</td>
            <td class="num"><b>{{ fmt(b.payableAmount) }}</b> 元</td>
            <td><span class="tag" :class="b.status.code==='SETTLED'?'green':'amber'">{{ lbl(b.status) }}</span></td>
            <td>
              <button class="mini" @click="selId=b.id">查看</button>
              <button v-if="b.status.code==='DRAFT'" class="mini" @click="settle(b.id)">结算关账</button>
            </td>
          </tr>
          <tr v-if="!bills.length"><td colspan="7" class="muted">暂无账单，请在「批次作业」中生成</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="cur" class="panel" style="margin:0">
      <h3>{{ cur.billNo }} 明细</h3>
      <div class="kv" style="margin-bottom:12px">
        <div class="k">批次</div><div>{{ cur.batch.batchNo }} · {{ cur.batch.category.name }} · {{ cur.owner.name }}</div>
        <div class="k">结算计价重量</div><div>{{ fmt(cur.settleWeight) }}kg（{{ lbl(cur.settleMethod) }}）</div>
        <div class="k">结算日</div><div>{{ cur.settleDate || '未结算' }}</div>
      </div>
      <table>
        <thead><tr><th>项目</th><th>说明</th><th class="num">重量kg</th><th class="num">单价</th><th class="num">金额(元)</th></tr></thead>
        <tbody>
          <tr v-for="(l, i) in cur.lines" :key="i">
            <td><span class="tag" :class="lineColor(l.lineType)">{{ lineName(l.lineType) }}</span></td>
            <td>{{ l.summary }}</td>
            <td class="num">{{ l.weight ? fmt(l.weight) : '-' }}</td>
            <td class="num">{{ l.unitPrice ?? '-' }}</td>
            <td class="num" :style="l.amount < 0 ? 'color:var(--danger)' : ''">{{ fmt(l.amount) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr><td colspan="4" style="text-align:right"><b>货值合计</b></td><td class="num">{{ fmt(cur.goodsAmount) }}</td></tr>
          <tr><td colspan="4" style="text-align:right">仓储费</td><td class="num" style="color:var(--danger)">-{{ fmt(cur.storageFee) }}</td></tr>
          <tr><td colspan="4" style="text-align:right">分拣费（含工时）</td><td class="num" style="color:var(--danger)">-{{ fmt(cur.sortFee) }}</td></tr>
          <tr><td colspan="4" style="text-align:right">损耗扣款</td><td class="num" style="color:var(--danger)">-{{ fmt(cur.lossDeduction) }}</td></tr>
          <tr><td colspan="4" style="text-align:right">退货扣款</td><td class="num" style="color:var(--danger)">-{{ fmt(cur.returnDeduction) }}</td></tr>
          <tr><td colspan="4" style="text-align:right">投诉赔付</td><td class="num" style="color:var(--danger)">-{{ fmt(cur.complaintDeduction) }}</td></tr>
          <tr><td colspan="4" style="text-align:right">农户预付款抵扣</td><td class="num" style="color:var(--danger)">-{{ fmt(cur.advanceDeduction) }}</td></tr>
          <tr style="background:#f2f8f5"><td colspan="4" style="text-align:right"><b>应付货主</b></td>
            <td class="num"><b style="font-size:16px">{{ fmt(cur.payableAmount) }}</b></td></tr>
        </tfoot>
      </table>
      <p class="muted" style="margin-top:10px">
        说明：按实际可售重量时货值取实际出库成交（含锁价）+ 在库余货参考价估值；按原重结算时以到货原重×初评等级价计价，
        并单独扣回退货与货主责任损耗；冷库/承运责任损耗不向货主扣款。仓储费按库位分段（含转仓前后，新库位自转仓日起算）。
      </p>
    </div>
    <div v-else class="panel muted">选择左侧账单查看明细。</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api, lbl, toast } from '../api'

const bills = ref([])
const selId = ref(null)
const cur = ref(null)

async function load() {
  bills.value = await api.get('/api/bills')
  if (selId.value) await select(selId.value)
}
async function select(id) {
  selId.value = id
  cur.value = await api.get('/api/bills/' + id)
}
async function settle(id) {
  if (!confirm('确认结算并关账？预付款余额将相应抵扣，批次将关闭。')) return
  try {
    await api.post('/api/bills/' + id + '/settle', {})
    toast('已结算关账', true)
    await load()
  } catch (e) { toast(e.message) }
}
function fmt(v) { return v == null ? '0' : Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) }
function lineName(t) {
  return { GOODS: '货值', STORAGE: '仓储费', SORT: '分拣费', LOSS: '损耗',
    RETURN: '退货', COMPLAINT: '投诉赔付', ADVANCE: '预付款' }[t] || t
}
function lineColor(t) {
  return { GOODS: 'green', STORAGE: 'amber', SORT: 'amber', LOSS: 'red',
    RETURN: 'red', COMPLAINT: 'red', ADVANCE: 'purple' }[t] || ''
}
onMounted(load)
</script>
