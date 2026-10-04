<template>
  <div class="page">
    <el-card shadow="never">
      <div class="card-title">客户投诉与责任裁定（坏果 / 短重 / 温度失控）</div>
      <el-table :data="list" size="small">
        <el-table-column prop="complaintNo" label="投诉单号" width="170" class="mono" />
        <el-table-column label="客户" width="160">
          <template #default="{ row }">{{ customerName(row.customerId) }}</template>
        </el-table-column>
        <el-table-column label="批次" width="170">
          <template #default="{ row }">{{ batchNo(row.batchId) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="danger">{{ $dict('ComplaintType', row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="problemWeight" label="问题kg" width="80" />
        <el-table-column prop="claimAmount" label="索赔元" width="80" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="责任裁定" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.resolved" size="small" :type="row.responsibility === 'WAREHOUSE' ? 'danger' : 'warning'">
              {{ $dict('Responsibility', row.responsibility) }}
            </el-tag>
            <el-tag v-else size="small" type="info">待裁定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="settledAmount" label="赔付元" width="80" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button v-if="!row.resolved" link type="primary" @click="open(row)">裁定</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="投诉责任裁定" width="500px">
      <el-form :model="form" label-width="110px" v-if="target">
        <el-form-item label="索赔情况">
          {{ target.problemWeight }}kg / 索赔 {{ target.claimAmount }} 元 — {{ target.description }}
        </el-form-item>
        <el-form-item label="责任方">
          <el-radio-group v-model="form.responsibility">
            <el-radio value="OWNER">货主责任（货款扣赔）</el-radio>
            <el-radio value="WAREHOUSE">冷库责任（冷库对客赔付）</el-radio>
            <el-radio value="CARRIER">承运责任</el-radio>
            <el-radio value="CUSTOMER">客户责任（免赔）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="裁定赔付元">
          <el-input-number v-model="form.settledAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="处理人">
          <el-input v-model="form.handler" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="form.handleResult" type="textarea" :rows="3"
                    placeholder="追溯结论：到货温度/在仓温控/出库交接情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">确认裁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const list = ref([])
const customers = ref([])
const batches = ref([])
const visible = ref(false)
const target = ref(null)
const form = reactive({ responsibility: 'OWNER', settledAmount: 0, handler: '', handleResult: '' })

function customerName(id) { return customers.value.find(c => c.id === id)?.name || '#' + id }
function batchNo(id) { return batches.value.find(b => b.id === id)?.batchNo || '#' + id }

function open(row) {
  target.value = row
  Object.assign(form, {
    responsibility: 'OWNER', settledAmount: Number(row.claimAmount || 0),
    handler: '', handleResult: ''
  })
  visible.value = true
}

async function submit() {
  await api.post(`/api/complaints/${target.value.id}/resolve`, form)
  ElMessage.success('裁定完成，账单与追溯链已更新')
  visible.value = false
  load()
}

async function load() {
  list.value = await api.get('/api/complaints')
}

onMounted(async () => {
  const [c, b] = await Promise.all([
    api.get('/api/masters/customers'),
    api.get('/api/batches')
  ])
  customers.value = c
  batches.value = b
  load()
})
</script>
