<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never">
          <div class="card-title">
            温区看板
            <el-tag size="small" type="info">故障温区只出不进</el-tag>
          </div>
          <el-row :gutter="12">
            <el-col :span="8" v-for="z in zones" :key="z.id">
              <div class="zone-card" :class="{ fault: z.fault }">
                <div class="z-head">
                  <span>{{ z.name }}</span>
                  <el-tag v-if="z.fault" type="danger" size="small">故障</el-tag>
                </div>
                <div class="z-temp">
                  <span class="t">{{ z.currentTemp }}℃</span>
                  <span class="hint">设定 {{ z.targetTemp }}℃ · {{ $dict('ZoneType', z.type) }}</span>
                </div>
                <el-progress :percentage="usagePct(z)" :status="z.fault ? 'exception' : ''"
                             :stroke-width="12" />
                <div class="hint">{{ z.occupiedKg }} / {{ z.capacityKg }} kg · {{ z.lotCount }} 个货位批次</div>
                <div v-if="z.faultNote" class="fault-note">⚠ {{ z.faultNote }}</div>
                <div style="margin-top:8px">
                  <el-button size="small" :type="z.fault ? 'success' : 'danger'" @click="toggleFault(z)">
                    {{ z.fault ? '修复温区' : '上报故障' }}
                  </el-button>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="never" style="margin-top:12px">
          <div class="card-title">库位列表与占用</div>
          <el-table :data="locations" size="small">
            <el-table-column prop="code" label="库位编码" width="140" class="mono" />
            <el-table-column label="温区" width="150">
              <template #default="{ row }">{{ zoneName(row.zoneId) }}</template>
            </el-table-column>
            <el-table-column prop="capacityKg" label="容量kg" width="90" />
            <el-table-column prop="occupiedKg" label="占用kg" width="90" />
            <el-table-column label="利用率">
              <template #default="{ row }">
                <el-progress :percentage="locPct(row)" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <div class="card-title">待上架（分拣/质检产出）</div>
          <el-table :data="pendingLots" size="small">
            <el-table-column label="批次" width="160">
              <template #default="{ row }">{{ batchNo(row.batchId) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="80">
              <template #default="{ row }">{{ $dict('Grade', row.grade) }}</template>
            </el-table-column>
            <el-table-column prop="currentWeight" label="重量kg" width="80" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openPut(row)">上架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" style="margin-top:12px">
          <div class="card-title">在库货位批次（转仓 / 报损）</div>
          <el-table :data="inLots" size="small" max-height="420">
            <el-table-column label="批次" width="150">
              <template #default="{ row }">{{ batchNo(row.batchId) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="70">
              <template #default="{ row }">{{ $dict('Grade', row.grade) }}</template>
            </el-table-column>
            <el-table-column prop="currentWeight" label="现重kg" width="75" />
            <el-table-column label="库位" width="105">
              <template #default="{ row }">{{ locCode(row.locationId) }}</template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button link type="warning" size="small" @click="openTransfer(row)">转仓</el-button>
                <el-button link type="danger" size="small" @click="openLoss(row)">报损</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 上架弹窗 -->
    <el-dialog v-model="putVisible" title="上架到库位" width="480px">
      <el-form label-width="90px" v-if="putTarget">
        <el-form-item label="货位批次">{{ putTarget.lotNo }}（{{ $dict('Grade', putTarget.grade) }}
          {{ putTarget.currentWeight }}kg）</el-form-item>
        <el-form-item label="目标库位">
          <el-select v-model="putForm.locationId" style="width:100%">
            <el-option v-for="l in availableLocations" :key="l.id"
                       :label="`${l.code}（${zoneName(l.zoneId)}，可用 ${l.capacityKg - l.occupiedKg}kg）`"
                       :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="putForm.operator" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="putVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPut">确认上架</el-button>
      </template>
    </el-dialog>

    <!-- 转仓弹窗 -->
    <el-dialog v-model="transferVisible" title="转仓移库" width="480px">
      <el-form label-width="90px" v-if="transferTarget">
        <el-form-item label="当前位置">
          {{ locCode(transferTarget.locationId) }}（{{ zoneName(transferTarget.zoneId) }}）
        </el-form-item>
        <el-form-item label="目标库位">
          <el-select v-model="transferForm.toLocationId" style="width:100%">
            <el-option v-for="l in locations.filter(x => x.id !== transferTarget.locationId)" :key="l.id"
                       :label="`${l.code}（${zoneName(l.zoneId)}，可用 ${l.capacityKg - l.occupiedKg}kg）`"
                       :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="transferForm.remark" type="textarea" :rows="2"
                    placeholder="温区故障/保鲜策略调整" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转仓</el-button>
      </template>
    </el-dialog>

    <!-- 报损弹窗 -->
    <el-dialog v-model="lossVisible" title="库内报损" width="480px">
      <el-form label-width="90px" v-if="lossTarget">
        <el-form-item label="货位批次">
          {{ lossTarget.lotNo }} {{ $dict('Grade', lossTarget.grade) }}
          在库 {{ lossTarget.currentWeight }}kg
        </el-form-item>
        <el-form-item label="报损重量kg">
          <el-input-number v-model="lossForm.weight" :min="0.1"
                           :max="Number(lossTarget.currentWeight)" :precision="2" />
        </el-form-item>
        <el-form-item label="责任方">
          <el-radio-group v-model="lossForm.responsibility">
            <el-radio value="WAREHOUSE">冷库责任（补偿货主）</el-radio>
            <el-radio value="OWNER">货主责任（自行承担）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="lossForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lossVisible = false">取消</el-button>
        <el-button type="danger" @click="submitLoss">确认报损</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const zones = ref([])
const locations = ref([])
const pendingLots = ref([])
const inLots = ref([])
const batches = ref([])

const availableLocations = computed(() =>
  locations.value.filter(l => {
    const z = zones.value.find(x => x.id === l.zoneId)
    return z && !z.fault
  }))

function usagePct(z) {
  return z.capacityKg > 0 ? Math.min(100, Math.round(z.occupiedKg / z.capacityKg * 100)) : 0
}
function locPct(l) {
  return l.capacityKg > 0 ? Math.min(100, Math.round(l.occupiedKg / l.capacityKg * 100)) : 0
}
function zoneName(id) { return zones.value.find(z => z.id === id)?.name || '#' + id }
function locCode(id) { return locations.value.find(l => l.id === id)?.code || '-' }
function batchNo(id) { return batches.value.find(b => b.id === id)?.batchNo || '#' + id }

async function toggleFault(z) {
  if (!z.fault) {
    const { value } = await ElMessageBox.prompt('请描述故障情况', '上报温区故障', {
      confirmButtonText: '上报', cancelButtonText: '取消', inputValue: z.faultNote || ''
    }).catch(() => ({ value: null }))
    if (value === null) return
    await api.post(`/api/stock/zones/${z.id}/status`, { fault: true, faultNote: value })
    ElMessage.warning('已上报故障：该温区禁止上架/转入，受影响批次已告警')
  } else {
    await api.post(`/api/stock/zones/${z.id}/status`, { fault: false })
    ElMessage.success('温区已恢复正常')
  }
  load()
}

const putVisible = ref(false)
const putTarget = ref(null)
const putForm = reactive({ locationId: null, operator: '' })
function openPut(lot) {
  putTarget.value = lot
  putForm.locationId = null
  putForm.operator = ''
  putVisible.value = true
}
async function submitPut() {
  if (!putForm.locationId) return ElMessage.warning('请选择库位')
  await api.post('/api/stock/put-away', { lotId: putTarget.value.id, ...putForm })
  ElMessage.success('上架成功')
  putVisible.value = false
  load()
}

const transferVisible = ref(false)
const transferTarget = ref(null)
const transferForm = reactive({ toLocationId: null, remark: '', operator: '调度员' })
function openTransfer(lot) {
  transferTarget.value = lot
  transferForm.toLocationId = null
  transferForm.remark = ''
  transferVisible.value = true
}
async function submitTransfer() {
  if (!transferForm.toLocationId) return ElMessage.warning('请选择目标库位')
  await api.post('/api/stock/transfer', { lotId: transferTarget.value.id, ...transferForm })
  ElMessage.success('转仓成功')
  transferVisible.value = false
  load()
}

const lossVisible = ref(false)
const lossTarget = ref(null)
const lossForm = reactive({ weight: 0, responsibility: 'WAREHOUSE', remark: '', operator: '调度员' })
function openLoss(lot) {
  lossTarget.value = lot
  lossForm.weight = 0
  lossForm.responsibility = 'WAREHOUSE'
  lossForm.remark = ''
  lossVisible.value = true
}
async function submitLoss() {
  if (lossForm.weight <= 0) return ElMessage.warning('请填写报损重量')
  await api.post('/api/stock/loss', { lotId: lossTarget.value.id, ...lossForm })
  ElMessage.success('报损已记录')
  lossVisible.value = false
  load()
}

async function load() {
  const [zd, ld, pend, inn, bd] = await Promise.all([
    api.get('/api/dashboard/zones'),
    api.get('/api/masters/locations'),
    api.get('/api/stock/pending'),
    api.get('/api/stock/in-storage'),
    api.get('/api/batches')
  ])
  zones.value = zd
  locations.value = ld
  pendingLots.value = pend
  inLots.value = inn
  batches.value = bd
}

onMounted(load)
</script>

<style scoped>
.zone-card {
  border: 1px solid #e6ebf0;
  border-radius: 10px;
  padding: 14px;
  background: #fff;
}
.zone-card.fault { border-color: #d9001b; background: #fef0f0; }
.z-head { display: flex; justify-content: space-between; font-weight: 600; }
.z-temp { margin: 8px 0; }
.z-temp .t { font-size: 26px; font-weight: 700; color: #1f6f54; }
.zone-card.fault .z-temp .t { color: #d9001b; }
.hint { color: #8794a3; font-size: 12px; margin-left: 8px; }
.fault-note { color: #d9001b; font-size: 12px; margin-top: 6px; }
</style>
