<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="15">
        <el-card shadow="never">
          <div class="card-title">出库排单 / 执行</div>
          <el-radio-group v-model="tab" size="small" style="margin-bottom:10px" @change="loadOrders">
            <el-radio-button value="PLANNED">待出库（{{ orders.length }}）</el-radio-button>
            <el-radio-button value="OUT">已出库</el-radio-button>
            <el-radio-button value="">全部</el-radio-button>
          </el-radio-group>
          <el-table :data="orders" size="small">
            <el-table-column prop="orderNo" label="出库单号" width="170" class="mono" />
            <el-table-column label="客户" width="160">
              <template #default="{ row }">
                {{ customerName(row.customerId) }}
                <el-tag v-if="row.customerChanged" type="warning" size="small">临时改</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="批次" width="160">
              <template #default="{ row }">{{ batchNo(row.batchId) }}</template>
            </el-table-column>
            <el-table-column prop="totalWeight" label="重量kg" width="85" />
            <el-table-column prop="totalAmount" label="金额元" width="100" />
            <el-table-column label="标记" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.priority" type="danger" size="small">优先</el-tag>
                <el-tag v-if="row.priceLocked" type="warning" size="small">
                  锁价{{ row.lockedPrice }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态/操作">
              <template #default="{ row }">
                <template v-if="row.status === 'PLANNED'">
                  <el-tag type="warning" size="small">待出库</el-tag>
                  <el-button link type="primary" @click="execute(row)">执行出库</el-button>
                </template>
                <template v-else>
                  <el-tag type="success" size="small">已出库</el-tag>
                  <el-button link type="primary" @click="openComplaint(row)">投诉</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card shadow="never">
          <div class="card-title">新建出库排单</div>
          <el-form :model="form" label-width="92px">
            <el-form-item label="批次">
              <el-select v-model="form.batchId" filterable style="width:100%" @change="onBatchChange">
                <el-option v-for="b in outBatches" :key="b.id"
                           :label="`${b.batchNo} ${catName(b.categoryId)} 可售${b.netWeight - b.outWeight - b.lossWeight}kg`"
                           :value="b.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="客户">
              <el-select v-model="form.customerId" filterable style="width:100%">
                <el-option v-for="c in customers" :key="c.id"
                           :label="c.name + (c.processingChannel ? '（加工渠道）' : '')"
                           :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="优先出库">
              <el-switch v-model="form.priority" active-text="客户催货，优先排单" />
            </el-form-item>

            <el-table :data="form.items" size="small" border style="margin-bottom:10px">
              <el-table-column label="货位/等级" min-width="150">
                <template #default="{ row }">
                  <el-select v-model="row.lotId" size="small" @change="onLotChange(row)">
                    <el-option v-for="l in availableLots" :key="l.id"
                               :label="`${lotZone(l.locationId)} ${$dict('Grade', l.grade)} ${l.currentWeight}kg@${l.unitPrice}`"
                               :value="l.id" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="重量kg" width="130">
                <template #default="{ row }">
                  <el-input-number v-model="row.weight" :min="0" :precision="2" size="small"
                                   :max="lotWeight(row.lotId)" />
                </template>
              </el-table-column>
              <el-table-column label="单价" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small"
                                   placeholder="锁价" />
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" @click="form.items.push({ lotId: null, weight: 0, unitPrice: null })">
              添加货位
            </el-button>

            <el-form-item label="备注" style="margin-top:10px">
              <el-input v-model="form.remark" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="plan">排单</el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="complaintVisible" title="出库后客户投诉登记" width="520px">
      <el-form :model="complaintForm" label-width="100px" v-if="complaintOrder">
        <el-form-item label="出库单">{{ complaintOrder.orderNo }} → {{ customerName(complaintOrder.customerId) }}</el-form-item>
        <el-form-item label="投诉类型">
          <el-select v-model="complaintForm.type" style="width:100%">
            <el-option label="坏果腐损" value="BAD_FRUIT" />
            <el-option label="短斤少两" value="SHORT_WEIGHT" />
            <el-option label="温度失控" value="TEMP_ABUSE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题重量kg">
          <el-input-number v-model="complaintForm.problemWeight" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="索赔金额元">
          <el-input-number v-model="complaintForm.claimAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="complaintForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplaint">提交投诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const tab = ref('PLANNED')
const orders = ref([])
const customers = ref([])
const batches = ref([])
const categories = ref([])
const inLots = ref([])
const locations = ref([])
const zones = ref([])
const saving = ref(false)

const form = reactive({
  batchId: null, customerId: null, priority: false, remark: '',
  operator: '调度员', items: [{ lotId: null, weight: 0, unitPrice: null }]
})

const outBatches = computed(() =>
  batches.value.filter(b => ['IN_STORAGE', 'PARTIAL_OUT'].includes(b.status)))
const availableLots = computed(() =>
  inLots.value.filter(l => l.batchId === form.batchId && Number(l.currentWeight) > 0))

function customerName(id) { return customers.value.find(c => c.id === id)?.name || '#' + id }
function batchNo(id) { return batches.value.find(b => b.id === id)?.batchNo || '#' + id }
function catName(id) { return categories.value.find(c => c.id === id)?.name || id }
function lotZone(locId) {
  const loc = locations.value.find(l => l.id === locId)
  return loc ? loc.code : '?'
}
function lotWeight(lotId) {
  return inLots.value.find(l => l.id === lotId)?.currentWeight || 0
}
function onLotChange(row) {
  if (!row.unitPrice) {
    const lot = inLots.value.find(l => l.id === row.lotId)
    row.unitPrice = lot ? Number(lot.unitPrice) : null
  }
}
function onBatchChange() {
  form.items = [{ lotId: null, weight: 0, unitPrice: null }]
}

async function plan() {
  if (!form.batchId || !form.customerId) {
    ElMessage.warning('请选择批次与客户')
    return
  }
  const items = form.items.filter(i => i.lotId && Number(i.weight) > 0)
  if (!items.length) {
    ElMessage.warning('请添加出库货位与重量')
    return
  }
  saving.value = true
  try {
    const o = await api.post('/api/outbounds/plan', { ...form, items })
    ElMessage.success('排单成功 ' + o.orderNo
      + (o.customerChanged ? '（已触发临时改客户告警）' : '')
      + (o.priceLocked ? '（命中锁价）' : ''))
    Object.assign(form, { batchId: null, customerId: null, priority: false, remark: '',
      items: [{ lotId: null, weight: 0, unitPrice: null }] })
    await reload()
  } finally {
    saving.value = false
  }
}

async function execute(row) {
  await ElMessageBox.confirm(`确认执行出库 ${row.orderNo}？将扣减可售重量并释放库位。`, '执行出库', {
    type: 'warning'
  })
  const o = await api.post(`/api/outbounds/${row.id}/execute?operator=${encodeURIComponent('调度员')}`)
  ElMessage.success(`出库完成 ${o.totalWeight}kg`)
  reload()
}

const complaintVisible = ref(false)
const complaintOrder = ref(null)
const complaintForm = reactive({ type: 'BAD_FRUIT', problemWeight: 0, claimAmount: 0, description: '' })
function openComplaint(row) {
  complaintOrder.value = row
  Object.assign(complaintForm, { type: 'BAD_FRUIT', problemWeight: 0, claimAmount: 0, description: '' })
  complaintVisible.value = true
}
async function submitComplaint() {
  await api.post('/api/complaints', { outboundId: complaintOrder.value.id, ...complaintForm })
  ElMessage.success('投诉已登记，批次链已追加投诉节点')
  complaintVisible.value = false
}

async function loadOrders() {
  orders.value = await api.get('/api/outbounds' + (tab.value ? '?status=' + tab.value : ''))
}

async function reload() {
  await Promise.all([
    loadOrders(),
    api.get('/api/batches').then(d => (batches.value = d)),
    api.get('/api/stock/in-storage').then(d => (inLots.value = d))
  ])
}

onMounted(async () => {
  const [c, cat, loc] = await Promise.all([
    api.get('/api/masters/customers'),
    api.get('/api/masters/categories'),
    api.get('/api/masters/locations')
  ])
  customers.value = c
  categories.value = cat
  locations.value = loc
  reload()
})
</script>
