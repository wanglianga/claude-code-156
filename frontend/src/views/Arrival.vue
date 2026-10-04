<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="16">
        <el-card shadow="never">
          <div class="card-title">到货登记（批次链起点）</div>
          <el-form :model="form" label-width="118px" :disabled="saving">
            <el-divider content-position="left">货主与品类</el-divider>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="货主" required>
                  <el-select v-model="form.ownerId" placeholder="选择货主" style="width:100%">
                    <el-option v-for="o in owners" :key="o.id"
                               :label="`${o.name}（${ownerTypeText[o.type]}）`" :value="o.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="品类" required>
                  <el-select v-model="form.categoryId" placeholder="选择品类" style="width:100%"
                             @change="onCategoryChange">
                    <el-option v-for="c in categories" :key="c.id"
                               :label="`${c.name}（允许 ${c.minArrivalTemp}~${c.maxArrivalTemp}℃）`"
                               :value="c.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="产地">
                  <el-input v-model="form.origin" placeholder="乡镇/村组" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生产日期">
                  <el-date-picker v-model="form.productionDate" type="date" value-format="YYYY-MM-DD"
                                  style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">重量 / 温度 / 包装</el-divider>
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="毛重(kg)" required>
                  <el-input-number v-model="form.grossWeight" :min="0" :precision="2" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="皮重(kg)">
                  <el-input-number v-model="form.tareWeight" :min="0" :precision="2" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="净重(kg)">
                  <el-input :model-value="netWeight" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="到货温度℃" required>
                  <el-input-number v-model="form.arrivalTemp" :precision="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="包装方式">
                  <el-select v-model="form.packaging" allow-create filterable style="width:100%">
                    <el-option label="泡沫箱加冰" value="泡沫箱加冰" />
                    <el-option label="筐装" value="筐装" />
                    <el-option label="纸盒" value="纸盒" />
                    <el-option label="真空包装" value="真空包装" />
                    <el-option label="散装" value="散装" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-alert v-if="tempHint" :title="tempHint" :type="tempOk ? 'success' : 'error'"
                      :closable="false" style="margin-bottom:12px" />

            <el-divider content-position="left">检疫票证 / 车辆</el-divider>
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="检疫证号">
                  <el-input v-model="form.certificateNo" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="出具机构">
                  <el-input v-model="form.issuingOrg" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="证明日期">
                  <el-date-picker v-model="form.certIssueDate" type="date" value-format="YYYY-MM-DD"
                                  style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="车牌号">
                  <el-input v-model="form.vehicleNo" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="票证核验">
                  <el-switch v-model="form.certificateVerified" active-text="已核验" inactive-text="未核验" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="混批混装">
                  <el-switch v-model="form.mixedLoad" active-text="同车混装" inactive-text="专车专批" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">客户与结算</el-divider>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="预计客户">
                  <el-select v-model="form.plannedCustomerId" clearable style="width:100%">
                    <el-option v-for="c in customers" :key="c.id"
                               :label="c.name + (c.processingChannel ? '（加工渠道）' : '')"
                               :value="c.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结算方式" required>
                  <el-select v-model="form.settleMethod" style="width:100%">
                    <el-option label="代销结算" value="CONSIGN" />
                    <el-option label="保底收购" value="BUYOUT" />
                    <el-option label="货主自销代结" value="SELF" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="客户锁价(元/kg)">
                  <el-input-number v-model="form.lockedPrice" :min="0" :precision="2" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="按原重结算">
                  <el-switch v-model="form.settleByOriginalWeight"
                             active-text="货主要求按原重结算（损耗风险冷库承担）" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submit">登记到货</el-button>
              <el-button @click="reset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" style="margin-bottom:12px">
          <div class="card-title">登记后自动风控识别</div>
          <ul class="tips">
            <li>到货温度超出品类阈值 → <b>温度超标严重告警</b></li>
            <li>同车混批混装 → 分拣防错批、防交叉污染</li>
            <li>票证未核验 → 质检前禁止入库/分拣</li>
            <li>按原重结算 → 损耗争议前置预警</li>
          </ul>
        </el-card>
        <el-card shadow="never">
          <div class="card-title">今日已登记批次</div>
          <el-table :data="recent" size="small">
            <el-table-column prop="batchNo" label="批次号" class="mono" width="160" />
            <el-table-column label="品类">
              <template #default="{ row }">{{ catName(row.categoryId) }}</template>
            </el-table-column>
            <el-table-column prop="netWeight" label="净重" width="70" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const owners = ref([])
const customers = ref([])
const categories = ref([])
const recent = ref([])
const saving = ref(false)

const ownerTypeText = { FARMER: '农户', COOPERATIVE: '合作社', WHOLESALER: '批发商' }

const defaultForm = () => ({
  ownerId: null, categoryId: null, origin: '', productionDate: '',
  grossWeight: 1000, tareWeight: 0, arrivalTemp: 5, packaging: '泡沫箱加冰',
  certificateNo: '', issuingOrg: '县农检中心', certIssueDate: new Date().toISOString().slice(0, 10),
  certificateVerified: true, vehicleNo: '', mixedLoad: false,
  plannedCustomerId: null, settleMethod: 'CONSIGN', lockedPrice: null,
  settleByOriginalWeight: false, operator: '管理员'
})
const form = reactive(defaultForm())

const netWeight = computed(() =>
  Number(((form.grossWeight || 0) - (form.tareWeight || 0)).toFixed(2)))

const currentCategory = computed(() => categories.value.find(c => c.id === form.categoryId))
const tempOk = computed(() => {
  const c = currentCategory.value
  if (!c || form.arrivalTemp == null) return true
  return form.arrivalTemp >= Number(c.minArrivalTemp) && form.arrivalTemp <= Number(c.maxArrivalTemp)
})
const tempHint = computed(() => {
  const c = currentCategory.value
  if (!c) return '选择品类后显示到货温度阈值'
  return tempOk.value
    ? `温度合规：${c.name} 允许 ${c.minArrivalTemp}~${c.maxArrivalTemp}℃`
    : `温度超标：${c.name} 允许 ${c.minArrivalTemp}~${c.maxArrivalTemp}℃，登记后将产生严重告警！`
})

function onCategoryChange() {
  const c = currentCategory.value
  if (c) {
    form.arrivalTemp = Number(c.maxArrivalTemp)
    form.lockedPrice = null
  }
}

function catName(id) {
  return categories.value.find(c => c.id === id)?.name || id
}

async function submit() {
  if (!form.ownerId || !form.categoryId || !form.settleMethod) {
    ElMessage.warning('请完善货主、品类、结算方式')
    return
  }
  if (netWeight.value <= 0) {
    ElMessage.warning('净重必须大于 0')
    return
  }
  saving.value = true
  try {
    const b = await api.post('/api/batches/arrival', { ...form, grossWeight: form.grossWeight,
      tareWeight: form.tareWeight })
    ElMessage.success('登记成功：' + b.batchNo)
    await ElMessageBox.confirm('批次已登记，是否前往质检验收？', '下一步', {
      confirmButtonText: '去质检', cancelButtonText: '继续登记', type: 'success'
    }).then(() => router.push('/inspection?batchId=' + b.id)).catch(() => {})
    reset()
    loadRecent()
  } finally {
    saving.value = false
  }
}

function reset() {
  Object.assign(form, defaultForm())
}

async function loadRecent() {
  const all = await api.get('/api/batches?status=REGISTERED')
  recent.value = all.slice(0, 8)
}

onMounted(async () => {
  const [o, c, cat] = await Promise.all([
    api.get('/api/masters/owners'),
    api.get('/api/masters/customers'),
    api.get('/api/masters/categories')
  ])
  owners.value = o
  customers.value = c
  categories.value = cat
  loadRecent()
})
</script>

<style scoped>
.tips { padding-left: 18px; color: #5a6673; font-size: 13px; line-height: 2; }
</style>
