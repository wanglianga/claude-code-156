<template>
  <div>
    <div class="panel">
      <h3>冷库温区
        <span class="muted" style="font-weight:400;font-size:12px">（故障温区将自动写入所有在库批次链，并阻止继续入库，需调度转仓）</span>
      </h3>
      <table>
        <thead><tr><th>编码</th><th>名称</th><th>类型</th><th>目标温</th><th>容量kg</th><th>已用kg</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="z in m.zones" :key="z.id">
            <td>{{ z.code }}</td><td>{{ z.name }}</td><td>{{ z.zoneType }}</td>
            <td>{{ z.targetTemp }}℃</td><td class="num">{{ z.capacityKg }}</td><td class="num">{{ z.usedKg }}</td>
            <td><span class="tag" :class="z.faulty?'red':'green'">{{ z.faulty ? '故障：'+z.faultNote : '正常' }}</span></td>
            <td>
              <button v-if="!z.faulty" class="mini danger" @click="setFault(z, true)">上报故障</button>
              <button v-else class="mini" @click="setFault(z, false)">恢复正常</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="row" style="margin-top:12px">
        <div class="field"><label>编码</label><input v-model="znew.code" /></div>
        <div class="field"><label>名称</label><input v-model="znew.name" /></div>
        <div class="field"><label>类型</label>
          <select v-model="znew.zoneType"><option>冷藏</option><option>冰温</option><option>冷冻</option></select>
        </div>
        <div class="field"><label>目标温℃</label><input v-model.number="znew.targetTemp" type="number" step="1" /></div>
        <div class="field"><label>容量kg</label><input v-model.number="znew.capacityKg" type="number" /></div>
        <div class="field" style="display:flex;align-items:flex-end"><button @click="addZone">新增温区</button></div>
      </div>
    </div>

    <div class="grid2">
      <div class="panel" style="margin:0">
        <h3>货主（农户/合作社/批发商）与预付款</h3>
        <table>
          <thead><tr><th>编码</th><th>名称</th><th>类型</th><th>电话</th><th class="num">预付款余额</th><th>预付款</th></tr></thead>
          <tbody>
            <tr v-for="o in m.owners" :key="o.id">
              <td>{{ o.code }}</td><td>{{ o.name }}</td><td>{{ lbl(o.type) }}</td><td>{{ o.phone }}</td>
              <td class="num">{{ o.advanceBalance }}</td>
              <td><input v-model.number="adv[o.id]" type="number" style="width:90px;display:inline-block" placeholder="±金额" />
                <button class="mini" @click="addAdvance(o.id)">入账</button></td>
            </tr>
          </tbody>
        </table>
        <div class="row" style="margin-top:12px">
          <div class="field"><label>编码</label><input v-model="onew.code" /></div>
          <div class="field"><label>名称</label><input v-model="onew.name" /></div>
          <div class="field"><label>类型</label>
            <select v-model="onew.type"><option value="FARMER">农户</option><option value="COOP">合作社</option><option value="WHOLESALER">批发商</option></select>
          </div>
          <div class="field"><label>电话</label><input v-model="onew.phone" /></div>
          <div class="field" style="display:flex;align-items:flex-end"><button @click="addOwner">新增货主</button></div>
        </div>
      </div>

      <div class="panel" style="margin:0">
        <h3>批发客户</h3>
        <table>
          <thead><tr><th>名称</th><th>联系人</th><th>电话</th></tr></thead>
          <tbody><tr v-for="c in m.customers" :key="c.id"><td>{{ c.name }}</td><td>{{ c.contact }}</td><td>{{ c.phone }}</td></tr></tbody>
        </table>
        <div class="row" style="margin-top:12px">
          <div class="field"><label>名称</label><input v-model="cnew.name" /></div>
          <div class="field"><label>联系人</label><input v-model="cnew.contact" /></div>
          <div class="field"><label>电话</label><input v-model="cnew.phone" /></div>
          <div class="field" style="display:flex;align-items:flex-end"><button @click="addCustomer">新增客户</button></div>
        </div>
      </div>
    </div>

    <div class="grid2">
      <div class="panel">
        <h3>品类（温度限值/温区/分级售价/费率/货架期）</h3>
        <table>
          <thead><tr><th>品类</th><th>到货温限</th><th>温区</th><th>A/B/C价</th><th>仓储元/吨天</th><th>分拣元/吨</th><th>货架期</th></tr></thead>
          <tbody>
            <tr v-for="c in m.categories" :key="c.id">
              <td>{{ c.name }}</td><td>{{ c.maxArrivalTemp }}℃</td><td>{{ c.targetZoneType }}</td>
              <td>{{ c.priceA }}/{{ c.priceB }}/{{ c.priceC }}</td>
              <td class="num">{{ c.storageFeePerTonDay }}</td><td class="num">{{ c.sortFeePerTon }}</td>
              <td>{{ c.shelfLifeDays }}天</td>
            </tr>
          </tbody>
        </table>
        <div class="row" style="margin-top:12px">
          <div class="field"><label>名称</label><input v-model="catnew.name" /></div>
          <div class="field"><label>到货温限℃</label><input v-model.number="catnew.maxArrivalTemp" type="number" step="0.5" /></div>
          <div class="field"><label>温区</label>
            <select v-model="catnew.targetZoneType"><option>冷藏</option><option>冰温</option><option>冷冻</option></select>
          </div>
          <div class="field"><label>A/B/C价</label>
            <div style="display:flex;gap:6px">
              <input v-model.number="catnew.priceA" type="number" />
              <input v-model.number="catnew.priceB" type="number" />
              <input v-model.number="catnew.priceC" type="number" />
            </div>
          </div>
          <div class="field"><label>仓储费/吨天</label><input v-model.number="catnew.storageFeePerTonDay" type="number" step="0.5" /></div>
          <div class="field"><label>分拣费/吨</label><input v-model.number="catnew.sortFeePerTon" type="number" /></div>
          <div class="field"><label>货架期天</label><input v-model.number="catnew.shelfLifeDays" type="number" /></div>
          <div class="field" style="display:flex;align-items:flex-end"><button @click="addCategory">新增品类</button></div>
        </div>
      </div>

      <div class="panel">
        <h3>客户锁价单（已锁价客户调度优先）</h3>
        <table>
          <thead><tr><th>客户</th><th>品类</th><th>等级</th><th class="num">锁价元/kg</th><th>锁量kg</th><th>有效期</th></tr></thead>
          <tbody>
            <tr v-for="l in m.priceLocks" :key="l.id">
              <td>{{ l.customer.name }}</td><td>{{ l.category.name }}</td><td>{{ lbl(l.grade) }}</td>
              <td class="num">{{ l.lockedPrice }}</td><td class="num">{{ l.lockedQtyKg }}</td>
              <td>{{ l.validFrom }} ~ {{ l.validTo }}</td>
            </tr>
          </tbody>
        </table>
        <div class="row" style="margin-top:12px">
          <div class="field"><label>客户</label>
            <select v-model="lnew.customerId"><option v-for="c in m.customers" :key="c.id" :value="c.id">{{ c.name }}</option></select>
          </div>
          <div class="field"><label>品类</label>
            <select v-model="lnew.categoryId"><option v-for="c in m.categories" :key="c.id" :value="c.id">{{ c.name }}</option></select>
          </div>
          <div class="field"><label>等级</label><select v-model="lnew.grade"><option>A</option><option>B</option><option>C</option></select></div>
          <div class="field"><label>锁价</label><input v-model.number="lnew.lockedPrice" type="number" step="0.5" /></div>
          <div class="field"><label>锁量kg</label><input v-model.number="lnew.lockedQtyKg" type="number" /></div>
          <div class="field"><label>起/止</label>
            <div style="display:flex;gap:6px"><input v-model="lnew.validFrom" type="date" /><input v-model="lnew.validTo" type="date" /></div>
          </div>
          <div class="field" style="display:flex;align-items:flex-end"><button @click="addLock">新增锁价</button></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { api, lbl, toast } from '../api'

const m = ref({ owners: [], customers: [], categories: [], zones: [], priceLocks: [] })
const adv = reactive({})

async function load() { m.value = await api.get('/api/meta') }

async function setFault(z, faulty) {
  let note = z.faultNote || ''
  if (faulty) note = prompt('请输入故障原因', '制冷设备故障') || ''
  try {
    await api.put(`/api/zones/${z.id}/fault`, { faulty, note })
    toast(faulty ? '已上报温区故障并通知在库批次' : '温区已恢复', true)
    await load()
  } catch (e) { toast(e.message) }
}
async function addAdvance(id) {
  if (!adv[id]) return
  try {
    await api.post(`/api/owners/${id}/advance`, { amount: Number(adv[id]) })
    adv[id] = null
    toast('预付款已入账，结算时自动抵扣', true)
    await load()
  } catch (e) { toast(e.message) }
}

const znew = reactive({ code: '', name: '', zoneType: '冷藏', targetTemp: 4, capacityKg: 1000 })
const onew = reactive({ code: '', name: '', type: 'FARMER', phone: '' })
const cnew = reactive({ name: '', phone: '', contact: '' })
const catnew = reactive({ name: '', maxArrivalTemp: 8, targetZoneType: '冷藏',
  priceA: 10, priceB: 7, priceC: 4, storageFeePerTonDay: 4, sortFeePerTon: 60, shelfLifeDays: 7 })
const lnew = reactive({ customerId: null, categoryId: null, grade: 'A', lockedPrice: 10,
  lockedQtyKg: 0, validFrom: today(), validTo: today(30) })

function today(offset = 0) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return d.toISOString().slice(0, 10)
}

async function addZone() {
  try { await api.post('/api/zones', { ...znew }); toast('温区已新增', true); Object.assign(znew, { code: '', name: '' }); await load() }
  catch (e) { toast(e.message) }
}
async function addOwner() {
  try { await api.post('/api/owners', { ...onew }); toast('货主已新增', true); onew.name = ''; await load() }
  catch (e) { toast(e.message) }
}
async function addCustomer() {
  try { await api.post('/api/customers', { ...cnew }); toast('客户已新增', true); cnew.name = ''; await load() }
  catch (e) { toast(e.message) }
}
async function addCategory() {
  try { await api.post('/api/categories', { ...catnew }); toast('品类已新增', true); catnew.name = ''; await load() }
  catch (e) { toast(e.message) }
}
async function addLock() {
  try { await api.post('/api/price-locks', { ...lnew }); toast('锁价单已新增', true); await load() }
  catch (e) { toast(e.message) }
}

onMounted(load)
</script>
