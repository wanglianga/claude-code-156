<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="4" v-for="s in stats" :key="s.label">
        <el-card class="stat-card" shadow="never">
          <div class="num" :style="{ color: s.color || '#1f6f54' }">{{ s.value }}</div>
          <div class="label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="10">
        <el-card shadow="never">
          <div class="card-title">批次状态分布</div>
          <div ref="pieEl" style="height:260px"></div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <div class="card-title">温区占用 / 故障</div>
          <div ref="barEl" style="height:260px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="14">
        <el-card shadow="never">
          <div class="card-title">
            调度建议
            <el-tag size="small" type="info">高风险先出 · 锁价客户优先 · 转加工渠道</el-tag>
          </div>
          <el-table :data="suggestions" size="small" max-height="300">
            <el-table-column label="动作" width="130">
              <template #default="{ row }">
                <el-tag v-if="row.action === 'URGENT_OUT'" type="danger">紧急出库</el-tag>
                <el-tag v-else-if="row.action === 'LOCK_FULFILL'" type="warning">优先履约锁价</el-tag>
                <el-tag v-else type="info">转加工渠道</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="batchNo" label="批次" width="170" class="mono" />
            <el-table-column prop="reason" label="原因" />
            <el-table-column prop="weight" label="重量kg" width="90" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <div class="card-title">
            未处理风险告警
            <el-button link type="primary" @click="loadAll">刷新</el-button>
          </div>
          <el-table :data="alerts" size="small" max-height="300">
            <el-table-column label="级别" width="64">
              <template #default="{ row }">
                <el-tag :type="row.level === 'CRITICAL' ? 'danger' : 'warning'" size="small">
                  {{ row.level === 'CRITICAL' ? '严重' : '预警' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" />
            <el-table-column label="操作" width="70">
              <template #default="{ row }">
                <el-button link type="primary" @click="resolveAlert(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top:12px">
      <div class="card-title">在仓批次监控（停留天数 / 温区 / 可售重量变化 / 损耗责任 / 客户催货）</div>
      <el-table :data="monitor" size="small" :row-class-name="rowClass">
        <el-table-column prop="batchNo" label="批次号" width="170" class="mono" />
        <el-table-column prop="category" label="品类" width="130" />
        <el-table-column label="在仓天数" width="90">
          <template #default="{ row }">
            <span :class="row.stayDays >= 5 ? 'risk-critical' : ''">{{ row.stayDays }} 天</span>
          </template>
        </el-table-column>
        <el-table-column prop="netWeight" label="原净重" width="80" />
        <el-table-column prop="sellableWeight" label="当前可售" width="90">
          <template #default="{ row }">
            <strong>{{ row.sellableWeight }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="lossWeight" label="累计损耗" width="90">
          <template #default="{ row }">
            <span :class="row.lossWeight > 0 ? 'risk-warn' : ''">{{ row.lossWeight }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="outWeight" label="已出库" width="80" />
        <el-table-column label="等级货位/温区" min-width="260">
          <template #default="{ row }">
            <el-tag v-for="g in row.grades" :key="g.lotNo" size="small"
                    :type="g.zoneFault ? 'danger' : g.grade === 'A' ? 'success' : g.grade === 'B' ? 'warning' : 'info'"
                    style="margin:2px">
              {{ $dict('Grade', g.grade) }} {{ g.weight }}kg · {{ g.zone }}
              <span v-if="g.shelfLeftDays !== null"
                    :class="g.shelfLeftDays <= 1 ? 'risk-critical' : ''">
                （余{{ g.shelfLeftDays }}天）
              </span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="客户" width="150">
          <template #default="{ row }">
            <div v-if="row.urgentCustomer">
              <el-tag type="danger" size="small">催货·{{ row.urgentCustomer }}</el-tag>
            </div>
            <div v-else-if="row.plannedCustomer" class="ok-text">{{ row.plannedCustomer }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/batches/' + row.batchId)">批次链</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import api from '../api'
import { ENUMS } from '../constants'

const overview = ref({})
const alerts = ref([])
const monitor = ref([])
const suggestions = ref([])
const zones = ref([])

const PALETTE = ['#1f6f54', '#4b9e7e', '#e6a23c', '#5b8ff9', '#d9001b', '#8d6fd9', '#9aa5b1']

const stats = computed(() => [
  { label: '批次总数', value: overview.value.totalBatches ?? 0 },
  { label: '在库重量(kg)', value: overview.value.inStoreWeight ?? 0 },
  { label: '待出库排单', value: overview.value.plannedOutboundOrders ?? 0, color: '#e6a23c' },
  { label: '客户催货', value: overview.value.priorityUrges ?? 0, color: '#d9001b' },
  { label: '未处理告警', value: overview.value.openAlerts ?? 0, color: '#d9001b' },
  { label: '履约中锁价', value: overview.value.activeLocks ?? 0, color: '#5b8ff9' }
])

const pieEl = ref(null)
const barEl = ref(null)
let pieChart, barChart

function rowClass({ row }) {
  return row.risk ? 'risk-row' : ''
}

async function resolveAlert(row) {
  await api.post(`/api/alerts/${row.id}/resolve`, { resolution: '已现场处理' })
  await loadAll()
}

async function loadAll() {
  overview.value = await api.get('/api/dashboard/overview')
  alerts.value = await api.get('/api/alerts/open')
  monitor.value = await api.get('/api/dashboard/storage-monitor')
  suggestions.value = await api.get('/api/dashboard/dispatch-suggestions')
  zones.value = await api.get('/api/dashboard/zones')
  await nextTick()
  renderPie()
  renderBar()
}

function renderPie() {
  if (!pieEl.value) return
  pieChart ||= echarts.init(pieEl.value)
  const counts = overview.value.batchStatusCount || {}
  pieChart.setOption({
    color: PALETTE,
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      label: { formatter: '{b} {c}' },
      data: Object.entries(counts).filter(([, v]) => v > 0)
        .map(([k, v]) => ({ name: ENUMS.BatchStatus[k], value: v }))
    }]
  })
}

function renderBar() {
  if (!barEl.value) return
  barChart ||= echarts.init(barEl.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['占用(kg)', '容量(kg)'], top: 0 },
    grid: { left: 50, right: 20, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: zones.value.map(z => z.name) },
    yAxis: { type: 'value' },
    series: [
      {
        name: '占用(kg)', type: 'bar', barWidth: 26,
        itemStyle: { color: (p) => zones.value[p.dataIndex].fault ? '#d9001b' : '#1f6f54' },
        label: { show: true, position: 'top', formatter: '{c}' },
        data: zones.value.map(z => z.occupiedKg)
      },
      {
        name: '容量(kg)', type: 'bar', barWidth: 26,
        itemStyle: { color: '#cfe0d9' },
        data: zones.value.map(z => z.capacityKg)
      }
    ]
  })
}

function onResize() {
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
:deep(.risk-row) { background: #fef0f0 !important; }
</style>
