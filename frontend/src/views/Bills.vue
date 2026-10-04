<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="card-title">批次（生成账单对象）</div>
          <el-input v-model="keyword" placeholder="搜索批次号" size="small" style="margin-bottom:8px" />
          <el-table :data="filteredBatches" size="small" highlight-current-row max-height="560"
                    @current-change="selectBatch">
            <el-table-column prop="batchNo" label="批次号" class="mono" width="175" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small">{{ $dict('BatchStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card shadow="never" v-if="batch">
          <div class="card-title">
            {{ batch.batchNo }} — 财务账单
            <el-tag size="small">{{ $dict('BatchStatus', batch.status) }}</el-tag>
          </div>
          <el-descriptions :column="4" size="small" border style="margin-bottom:12px">
            <el-descriptions-item label="货主">{{ ownerName(batch.ownerId) }}</el-descriptions-item>
            <el-descriptions-item label="品类">{{ catName(batch.categoryId) }}</el-descriptions-item>
            <el-descriptions-item label="净重">{{ batch.netWeight }}kg</el-descriptions-item>
            <el-descriptions-item label="预付余额">{{ ownerAdvance }}元</el-descriptions-item>
            <el-descriptions-item label="可售重量">{{ batch.acceptedWeight }}kg</el-descriptions-item>
            <el-descriptions-item label="已出库">{{ batch.outWeight }}kg</el-descriptions-item>
            <el-descriptions-item label="损耗">{{ batch.lossWeight }}kg</el-descriptions-item>
            <el-descriptions-item label="退货">{{ batch.returnedWeight }}kg</el-descriptions-item>
          </el-descriptions>

          <el-button type="primary" size="small" @click="generate" :loading="loading">
            生成/重算账单
          </el-button>
          <el-tag v-if="bill" size="small" style="margin-left:8px">{{ $dict('BillStatus', bill.status) }}</el-tag>

          <template v-if="bill">
            <el-row :gutter="10" style="margin:12px 0">
              <el-col :span="6">
                <div class="money-card">
                  <div class="mlabel">销售/补偿货值</div>
                  <div class="mnum">{{ bill.grossValue }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="money-card warn">
                  <div class="mlabel">费用扣款（含预付抵扣）</div>
                  <div class="mnum">{{ bill.totalDeduction }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="money-card ok">
                  <div class="mlabel">应付货主净额</div>
                  <div class="mnum">{{ bill.netPayable }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="money-card">
                  <div class="mlabel">预付款抵扣 / 实付</div>
                  <div class="mnum">{{ bill.advanceDeducted }} / {{ bill.paidAmount || 0 }}</div>
                </div>
              </el-col>
            </el-row>

            <el-table :data="items" size="small" border>
              <el-table-column label="类型" width="140">
                <template #default="{ row }">{{ $dict('BillItemType', row.type) }}</template>
              </el-table-column>
              <el-table-column prop="summary" label="说明" min-width="300" />
              <el-table-column prop="weight" label="重量kg" width="90" />
              <el-table-column prop="unitPrice" label="单价/费率" width="100" />
              <el-table-column label="金额元" width="110">
                <template #default="{ row }">
                  <span :class="Number(row.amount) < 0 ? 'risk-warn' : 'ok-text'">{{ row.amount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="天数" width="60">
                <template #default="{ row }">{{ row.days || '' }}</template>
              </el-table-column>
            </el-table>

            <div style="margin-top:14px">
              <el-button v-if="bill.status === 'DRAFT'" type="warning" @click="confirmBill">确认账单</el-button>
              <el-button v-if="bill.status === 'CONFIRMED'" type="success" @click="pay">结算付款</el-button>
              <el-button @click="$router.push('/batches/' + batch.id)">查看批次链</el-button>
            </div>
          </template>
        </el-card>
        <el-empty v-else description="选择左侧批次生成账单" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const batches = ref([])
const owners = ref([])
const categories = ref([])
const keyword = ref('')
const batch = ref(null)
const bill = ref(null)
const items = ref([])
const loading = ref(false)

const filteredBatches = computed(() =>
  batches.value.filter(b => !keyword.value || b.batchNo.includes(keyword.value.trim())))
const ownerName = (id) => owners.value.find(o => o.id === id)?.name || '#' + id
const catName = (id) => categories.value.find(c => c.id === id)?.name || id
const ownerAdvance = computed(() =>
  owners.value.find(o => o.id === batch.value?.ownerId)?.advanceBalance ?? 0)

async function selectBatch(row) {
  if (!row) return
  batch.value = row
  const list = await api.get('/api/bills')
  const existing = list.filter(b => b.batchId === row.id)
    .sort((a, b2) => (b2.status === 'PAID' ? 1 : 0) - (a.status === 'PAID' ? 1 : 0))[0]
  if (existing) {
    bill.value = existing
    items.value = await api.get(`/api/bills/${existing.id}/items`)
  } else {
    bill.value = null
    items.value = []
  }
}

async function generate() {
  loading.value = true
  try {
    bill.value = await api.post('/api/bills/generate', { batchId: batch.value.id })
    items.value = await api.get(`/api/bills/${bill.value.id}/items`)
    ElMessage.success('账单已按当前批次链数据重算')
    owners.value = await api.get('/api/masters/owners')
  } finally {
    loading.value = false
  }
}

async function confirmBill() {
  bill.value = await api.post(`/api/bills/${bill.value.id}/confirm`)
  ElMessage.success('账单已确认')
}

async function pay() {
  const { value } = await ElMessageBox.prompt('实付金额（现金/转账）', '结算付款', {
    inputValue: String(bill.value.netPayable),
    inputPattern: /^\d+(\.\d{1,2})?$/,
    inputErrorMessage: '请输入正确金额'
  })
  bill.value = await api.post(`/api/bills/${bill.value.id}/settle`, { paidAmount: Number(value) })
  items.value = await api.get(`/api/bills/${bill.value.id}/items`)
  owners.value = await api.get('/api/masters/owners')
  ElMessage.success('结算完成，预付款已抵扣落账')
}

onMounted(async () => {
  const [b, o, c] = await Promise.all([
    api.get('/api/batches'),
    api.get('/api/masters/owners'),
    api.get('/api/masters/categories')
  ])
  batches.value = b
  owners.value = o
  categories.value = c
})
</script>

<style scoped>
.money-card {
  background: #f7f9fa; border-radius: 10px; padding: 14px;
}
.money-card.warn { background: #fdf6ec; }
.money-card.ok { background: #e8f4ef; }
.mlabel { color: #7a8794; font-size: 12px; }
.mnum { font-size: 22px; font-weight: 700; margin-top: 6px; color: #1f2d3d; }
</style>
