<template>
  <div class="panel" style="max-width:760px">
    <h2>到货登记</h2>
    <p class="muted">农户/合作社/批发商送货到库时登记车、货主、品类、批次、产地、重量、到货温度、包装、检疫证、预计出库客户与结算方式。同一车货的预付款、锁价、分拣工时与低温仓位都将挂到该批次链上。</p>
    <div class="row">
      <div class="field"><label>车牌号</label><input v-model="f.vehicleNo" placeholder="如 皖A·12345" /></div>
      <div class="field">
        <label>货主</label>
        <select v-model="f.ownerId">
          <option value="" disabled>选择货主</option>
          <option v-for="o in meta.owners" :key="o.id" :value="o.id">
            {{ o.name }}（{{ lbl(o.type) }}，预付款余额 {{ o.advanceBalance }}元）
          </option>
        </select>
      </div>
    </div>
    <div class="row">
      <div class="field">
        <label>品类</label>
        <select v-model="f.categoryId" @change="onCat">
          <option value="" disabled>选择品类</option>
          <option v-for="c in meta.categories" :key="c.id" :value="c.id">
            {{ c.name }}（到货温度限值 {{ c.maxArrivalTemp }}℃，入{{ c.targetZoneType }}）
          </option>
        </select>
      </div>
      <div class="field"><label>产地</label><input v-model="f.origin" placeholder="如 长丰草莓基地" /></div>
    </div>
    <div class="row">
      <div class="field"><label>到货重量(kg)</label><input v-model.number="f.arrivalWeight" type="number" min="0" step="0.01" /></div>
      <div class="field">
        <label>到货温度(℃)</label>
        <input v-model.number="f.arrivalTemp" type="number" step="0.1" />
        <div v-if="tempWarn" class="tag red" style="margin-top:4px">⚠ 超过品类限值 {{ cat?.maxArrivalTemp }}℃，质检将重点判定</div>
      </div>
    </div>
    <div class="row">
      <div class="field"><label>包装</label>
        <select v-model="f.packaging">
          <option>泡沫箱</option><option>塑料筐</option><option>纸箱</option><option>编织袋</option>
          <option>散货</option><option>散冰</option><option>真空包装</option>
        </select>
      </div>
      <div class="field"><label>检疫证明编号</label><input v-model="f.quarantineNo" placeholder="如 JY2026100401" /></div>
    </div>
    <div class="row">
      <div class="field">
        <label>预计出库客户</label>
        <select v-model="f.plannedCustomerId">
          <option :value="null">未指定</option>
          <option v-for="c in meta.customers" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>结算方式</label>
        <select v-model="f.settleMethod">
          <option value="ACTUAL">按实际可售重量结算</option>
          <option value="ORIGINAL">货主要求按到货原重结算</option>
        </select>
      </div>
    </div>
    <div class="field">
      <label><input type="checkbox" style="width:auto" v-model="f.mixedLoad" /> 本车混批混装（不同产地/等级混装，需标记并写进批次链）</label>
    </div>
    <button @click="submit">登记到货</button>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api, lbl, toast } from '../api'
import { goto } from '../state'

const meta = ref({ owners: [], categories: [], customers: [] })
const f = reactive({
  vehicleNo: '', ownerId: null, categoryId: null, origin: '',
  arrivalWeight: 1000, arrivalTemp: null, packaging: '泡沫箱',
  quarantineNo: '', plannedCustomerId: null, settleMethod: 'ACTUAL', mixedLoad: false
})
const cat = computed(() => meta.value.categories.find(c => c.id === f.categoryId))
const tempWarn = computed(() => cat.value && f.arrivalTemp != null && Number(f.arrivalTemp) > Number(cat.value.maxArrivalTemp))

onMounted(async () => { meta.value = await api.get('/api/meta') })
function onCat() {
  if (cat.value) f.arrivalTemp = Number(cat.value.maxArrivalTemp) + 1
}

async function submit() {
  try {
    const b = await api.post('/api/batches/register', { ...f })
    toast(`已登记批次 ${b.batchNo}，请前往质检验收`, true)
    goto('ops', b.id)
  } catch (e) { toast(e.message) }
}
</script>
