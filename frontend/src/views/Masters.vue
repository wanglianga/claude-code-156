<template>
  <div class="page">
    <el-tabs v-model="tab" type="border-card">
      <!-- 货主 -->
      <el-tab-pane label="货主 / 预付款" name="owners">
        <el-button type="primary" size="small" @click="openOwner(null)" style="margin-bottom:10px">
          新增货主
        </el-button>
        <el-table :data="owners" size="small">
          <el-table-column prop="name" label="名称" width="180" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              {{ {FARMER:'农户',COOPERATIVE:'合作社',WHOLESALER:'批发商'}[row.type] }}
            </template>
          </el-table-column>
          <el-table-column prop="contact" label="联系人" width="110" />
          <el-table-column prop="phone" label="电话" width="140" />
          <el-table-column prop="village" label="村镇" width="180" />
          <el-table-column label="预付款应收余额" width="140">
            <template #default="{ row }">
              <b :class="row.advanceBalance > 0 ? 'risk-warn' : 'ok-text'">{{ row.advanceBalance }} 元</b>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAdvance(row)">预付款放款</el-button>
              <el-button link type="info" @click="viewLedger(row)">台账</el-button>
              <el-button link @click="openOwner(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 客户 -->
      <el-tab-pane label="客户 / 锁价协议" name="customers">
        <el-row :gutter="12">
          <el-col :span="11">
            <el-button type="primary" size="small" @click="openCustomer(null)" style="margin-bottom:10px">
              新增客户
            </el-button>
            <el-table :data="customers" size="small">
              <el-table-column prop="name" label="名称" width="200" />
              <el-table-column prop="contact" label="联系人" width="110" />
              <el-table-column prop="phone" label="电话" width="140" />
              <el-table-column label="加工渠道" width="90">
                <template #default="{ row }">
                  <el-tag v-if="row.processingChannel" size="small" type="success">加工</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作">
                <template #default="{ row }">
                  <el-button link @click="openCustomer(row)">编辑</el-button>
                  <el-button link type="warning" @click="openLock(row)">锁价</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-col>
          <el-col :span="13">
            <div class="card-title" style="margin-top:0">锁价协议（调度优先履约）</div>
            <el-table :data="locks" size="small">
              <el-table-column prop="lockNo" label="协议号" width="130" class="mono" />
              <el-table-column label="客户" width="150">
                <template #default="{ row }">{{ customerName(row.customerId) }}</template>
              </el-table-column>
              <el-table-column prop="categoryCode" label="品类" width="90" />
              <el-table-column prop="lockedPrice" label="锁价元" width="80" />
              <el-table-column label="履约量">
                <template #default="{ row }">
                  {{ row.fulfilledQty }} / {{ row.lockedQty }}kg
                </template>
              </el-table-column>
              <el-table-column label="有效期" width="200">
                <template #default="{ row }">{{ row.validFrom }} ~ {{ row.validTo }}</template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                    {{ {ACTIVE:'履约中',FULFILLED:'已履约',CANCELLED:'已取消'}[row.status] }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 品类 -->
      <el-tab-pane label="品类温区阈值 / 费率" name="categories">
        <el-table :data="categories" size="small">
          <el-table-column prop="code" label="编码" width="110" />
          <el-table-column prop="name" label="品类" width="160" />
          <el-table-column label="到货温区℃" width="120">
            <template #default="{ row }">{{ row.minArrivalTemp }} ~ {{ row.maxArrivalTemp }}</template>
          </el-table-column>
          <el-table-column label="建议温区" width="120">
            <template #default="{ row }">{{ $dict('ZoneType', row.recommendedZone) }}</template>
          </el-table-column>
          <el-table-column prop="storageFeeRate" label="仓储元/kg·天" width="120" />
          <el-table-column prop="sortFeeRate" label="分拣元/kg" width="100" />
          <el-table-column label="约定损耗" width="90">
            <template #default="{ row }">{{ (row.agreedLossRate*100).toFixed(1) }}%</template>
          </el-table-column>
          <el-table-column label="默认价 A/B/加工">
            <template #default="{ row }">
              {{ row.defaultPriceA }} / {{ row.defaultPriceB }} / {{ row.defaultPriceProcess }}
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 货主弹窗 -->
    <el-dialog v-model="ownerVisible" :title="ownerForm.id ? '编辑货主' : '新增货主'" width="520px">
      <el-form :model="ownerForm" label-width="110px">
        <el-form-item label="名称"><el-input v-model="ownerForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="ownerForm.type" style="width:100%">
            <el-option label="农户" value="FARMER" />
            <el-option label="合作社" value="COOPERATIVE" />
            <el-option label="批发商" value="WHOLESALER" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人"><el-input v-model="ownerForm.contact" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="ownerForm.phone" /></el-form-item>
        <el-form-item label="村镇"><el-input v-model="ownerForm.village" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ownerVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOwner">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预付款弹窗 -->
    <el-dialog v-model="advanceVisible" title="货主预付款放款" width="460px">
      <el-form :model="advanceForm" label-width="100px">
        <el-form-item label="货主">{{ advanceOwner?.name }}（当前余额 {{ advanceOwner?.advanceBalance }} 元）</el-form-item>
        <el-form-item label="放款金额元">
          <el-input-number v-model="advanceForm.amount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="关联批次ID">
          <el-input-number v-model="advanceForm.batchId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="advanceForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advanceVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAdvance">放款</el-button>
      </template>
    </el-dialog>

    <!-- 台账弹窗 -->
    <el-dialog v-model="ledgerVisible" title="预付款台账流水" width="600px">
      <el-table :data="ledger" size="small">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ row.occurredAt?.replace('T',' ').slice(0,16) }}</template>
        </el-table-column>
        <el-table-column label="方向" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.direction === 'ADVANCE' ? 'danger' : 'success'">
              {{ row.direction === 'ADVANCE' ? '放款' : '回收' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额元" width="100" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-dialog>

    <!-- 客户弹窗 -->
    <el-dialog v-model="customerVisible" :title="customerForm.id ? '编辑客户' : '新增客户'" width="500px">
      <el-form :model="customerForm" label-width="110px">
        <el-form-item label="名称"><el-input v-model="customerForm.name" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="customerForm.contact" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="customerForm.phone" /></el-form-item>
        <el-form-item label="加工渠道">
          <el-switch v-model="customerForm.processingChannel" active-text="降级货可转入该客户" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customerVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomer">保存</el-button>
      </template>
    </el-dialog>

    <!-- 锁价弹窗 -->
    <el-dialog v-model="lockVisible" title="新建锁价协议" width="500px">
      <el-form :model="lockForm" label-width="100px">
        <el-form-item label="客户">{{ lockCustomer?.name }}</el-form-item>
        <el-form-item label="品类">
          <el-select v-model="lockForm.categoryCode" style="width:100%">
            <el-option v-for="c in categories" :key="c.code" :label="c.name" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="锁价元/kg">
          <el-input-number v-model="lockForm.lockedPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="锁定量kg">
          <el-input-number v-model="lockForm.lockedQty" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="lockRange" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="lockForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lockVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLock">创建协议</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const tab = ref('owners')
const owners = ref([])
const customers = ref([])
const categories = ref([])
const locks = ref([])

function customerName(id) { return customers.value.find(c => c.id === id)?.name || '#' + id }

async function reload() {
  const [o, c, cat, l] = await Promise.all([
    api.get('/api/masters/owners'),
    api.get('/api/masters/customers'),
    api.get('/api/masters/categories'),
    api.get('/api/price-locks')
  ])
  owners.value = o
  customers.value = c
  categories.value = cat
  locks.value = l
}

const ownerVisible = ref(false)
const ownerForm = reactive({ id: null, name: '', type: 'FARMER', contact: '', phone: '', village: '' })
function openOwner(row) {
  Object.assign(ownerForm, row ? { ...row } :
    { id: null, name: '', type: 'FARMER', contact: '', phone: '', village: '' })
  ownerVisible.value = true
}
async function saveOwner() {
  if (!ownerForm.name) return ElMessage.warning('请填写名称')
  await api.post('/api/masters/owners', { ...ownerForm })
  ElMessage.success('已保存')
  ownerVisible.value = false
  reload()
}

const advanceVisible = ref(false)
const advanceOwner = ref(null)
const advanceForm = reactive({ amount: 0, batchId: null, remark: '' })
function openAdvance(row) {
  advanceOwner.value = row
  Object.assign(advanceForm, { amount: 0, batchId: null, remark: '' })
  advanceVisible.value = true
}
async function saveAdvance() {
  if (advanceForm.amount <= 0) return ElMessage.warning('金额必须大于 0')
  await api.post(`/api/owners/${advanceOwner.value.id}/advance`, { ...advanceForm })
  ElMessage.success('预付款已放款')
  advanceVisible.value = false
  reload()
}

const ledgerVisible = ref(false)
const ledger = ref([])
async function viewLedger(row) {
  ledger.value = await api.get(`/api/owners/${row.id}/advances`)
  ledgerVisible.value = true
}

const customerVisible = ref(false)
const customerForm = reactive({ id: null, name: '', contact: '', phone: '', processingChannel: false })
function openCustomer(row) {
  Object.assign(customerForm, row ? { ...row } :
    { id: null, name: '', contact: '', phone: '', processingChannel: false })
  customerVisible.value = true
}
async function saveCustomer() {
  if (!customerForm.name) return ElMessage.warning('请填写名称')
  await api.post('/api/masters/customers', { ...customerForm })
  ElMessage.success('已保存')
  customerVisible.value = false
  reload()
}

const lockVisible = ref(false)
const lockCustomer = ref(null)
const lockRange = ref([])
const lockForm = reactive({ categoryCode: '', lockedPrice: 0, lockedQty: 0, remark: '' })
function openLock(row) {
  lockCustomer.value = row
  Object.assign(lockForm, { categoryCode: categories.value[0]?.code, lockedPrice: 0, lockedQty: 0, remark: '' })
  lockRange.value = []
  lockVisible.value = true
}
async function saveLock() {
  await api.post('/api/price-locks', {
    customerId: lockCustomer.value.id,
    ...lockForm,
    validFrom: lockRange.value?.[0] || null,
    validTo: lockRange.value?.[1] || null
  })
  ElMessage.success('锁价协议已创建')
  lockVisible.value = false
  reload()
}

onMounted(reload)
</script>
