<template>
  <div>
    <div class="cards" style="margin-bottom:16px">
      <div class="card"><div class="k">批次总数</div><div class="v">{{ d.totalBatches ?? '-' }}</div></div>
      <div class="card"><div class="k">在仓/部分出库</div><div class="v green">{{ d.inStorage ?? '-' }}</div></div>
      <div class="card"><div class="k">待分拣</div><div class="v">{{ d.sorting ?? '-' }}</div></div>
      <div class="card"><div class="k">已退货</div><div class="v red">{{ d.returned ?? '-' }}</div></div>
      <div class="card"><div class="k">已出清</div><div class="v">{{ d.shipped ?? '-' }}</div></div>
      <div class="card"><div class="k">已结算关账</div><div class="v">{{ d.closed ?? '-' }}</div></div>
      <div class="card"><div class="k">⚠ 高风险批次</div><div class="v red">{{ d.highRiskCount ?? '-' }}</div></div>
      <div class="card"><div class="k">在仓可售(kg)</div><div class="v">{{ fmt(d.totalSaleableKg) }}</div></div>
      <div class="card"><div class="k">累计损耗(kg)</div><div class="v red">{{ fmt(d.totalLossKg) }}</div></div>
      <div class="card"><div class="k">投诉赔付(元)</div><div class="v red">{{ fmt(d.totalComplaintAmount) }}</div></div>
    </div>

    <div class="grid2">
      <div class="panel">
        <h3>冷库温区占用 / 故障</h3>
        <table>
          <thead><tr><th>温区</th><th>类型</th><th>目标温</th><th>占用</th><th>使用率</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="z in d.zones" :key="z.id">
              <td>{{ z.name }}<div class="muted">{{ z.code }}</div></td>
              <td>{{ z.zoneType }}</td>
              <td>{{ z.targetTemp }}℃</td>
              <td class="num">{{ fmt(z.usedKg) }} / {{ fmt(z.capacityKg) }}kg</td>
              <td style="min-width:120px">
                <div class="progress" :class="z.usageRate > 90 ? 'danger' : z.usageRate > 70 ? 'warn' : ''">
                  <div :style="{ width: Math.min(100, z.usageRate) + '%' }"></div>
                </div>
                <span class="muted">{{ z.usageRate }}%</span>
              </td>
              <td>
                <span v-if="z.faulty" class="tag red">故障</span>
                <span v-else class="tag green">正常</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel">
        <h3>客户催货 / 优先出库</h3>
        <table>
          <thead><tr><th>批次</th><th>客户</th><th>要求</th><th>说明</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-if="!d.openDemands?.length"><td colspan="5" class="muted">暂无催货</td></tr>
            <tr v-for="dm in d.openDemands" :key="dm.id">
              <td><a @click="openBatch(dm.batch.id)">{{ dm.batch.batchNo }}</a></td>
              <td>{{ dm.customer.name }}</td>
              <td><span v-if="dm.priority" class="tag red">要求优先出库</span><span v-else class="tag">普通</span></td>
              <td>{{ dm.note }}</td>
              <td><span class="tag amber">{{ lbl(dm.status) }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="panel">
      <h3>批次调度队列
        <span class="muted" style="font-weight:400;font-size:12px">
          （排序：高风险 › 优先催货 › 已锁价 › 在仓停留天数；调度依据：先出高风险批次、先满足已锁价客户、必要时转加工渠道）
        </span>
        <button class="ghost mini" style="float:right" @click="load">刷新</button>
      </h3>
      <table>
        <thead>
          <tr>
            <th>批次号</th><th>货主</th><th>品类</th><th>状态</th><th>停留(天)</th><th>剩余货架期</th>
            <th>温区/库位</th><th>可售kg</th><th>已出kg</th><th>损耗kg</th><th>损耗率</th>
            <th>风险标记</th><th>调度建议</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in d.batches" :key="b.id" :style="b.highRisk ? 'background:#fff8f6' : ''">
            <td><a @click="openBatch(b.id)"><b>{{ b.batchNo }}</b></a><div class="muted">{{ b.vehicleNo }}</div></td>
            <td>{{ b.owner.name }}</td>
            <td>{{ b.category.name }}<div class="muted">{{ b.origin }}</div></td>
            <td><span class="tag" :class="statusColor(b.status.code)">{{ lbl(b.status) }}</span></td>
            <td class="num">{{ b.stayDays }}</td>
            <td class="num">
              <span :class="b.daysToExpiry <= 1 ? 'tag red' : b.nearExpiry ? 'tag amber' : ''">
                {{ b.inboundDate ? b.daysToExpiry + '天' : '-' }}
              </span>
            </td>
            <td>
              <span v-for="(z, i) in b.inZones" :key="i">
                <span class="tag" :class="z.faulty ? 'red' : 'blue'">
                  {{ z.zone }} {{ z.grade }}级 {{ fmt(z.weight) }}kg
                </span>
              </span>
              <span v-if="!b.inZones.length" class="muted">-</span>
            </td>
            <td class="num"><b>{{ fmt(b.saleableWeight) }}</b></td>
            <td class="num">{{ fmt(b.shippedWeight) }}</td>
            <td class="num" :class="b.lossWeight > 0 ? '' : ''">{{ fmt(b.lossWeight) }}</td>
            <td class="num">{{ b.lossRate }}%</td>
            <td>
              <span v-for="t in b.riskTags" :key="t"
                    class="tag" :class="riskColor(t)">{{ t }}</span>
            </td>
            <td>
              <span v-for="s in b.suggestions" :key="s" class="tag green">→ {{ s }}</span>
            </td>
            <td><button class="mini" @click="openBatch(b.id)">作业/追溯</button></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api, lbl } from '../api'
import { goto } from '../state'

const d = ref({})
async function load() { d.value = await api.get('/api/dashboard') }
onMounted(load)

function fmt(v) {
  if (v == null) return '0'
  const n = Number(v)
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function openBatch(id) { goto('ops', id) }
function statusColor(c) {
  return { IN_STORAGE: 'blue', PARTIAL_OUT: 'amber', SHIPPED: 'green', RETURNED: 'red',
    SORTING: 'purple', REGISTERED: '', CLOSED: 'green' }[c] || ''
}
function riskColor(t) {
  if (t.includes('温度') || t.includes('故障') || t.includes('投诉') || t.includes('损耗')) return 'red'
  if (t.includes('催货') || t.includes('混批') || t.includes('货架期')) return 'amber'
  return 'blue'
}
</script>
