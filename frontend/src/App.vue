<template>
  <div v-if="!store.token" class="login-wrap">
    <div class="login-card">
      <div class="brand">🧊 县域农贸冷库</div>
      <h2>入库质检分拣出库结算平台</h2>
      <div class="field">
        <label>用户名</label>
        <input v-model="username" placeholder="admin / qc / warehouse / finance" @keyup.enter="login" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="password" type="password" placeholder="admin123" @keyup.enter="login" />
      </div>
      <button style="width:100%" @click="login">登 录</button>
      <p class="muted" style="margin-top:12px;line-height:1.7">
        演示账号：admin/admin123（管理员）、qc/qc123（质检）、warehouse/wh123（库管）、finance/fin123（财务）
      </p>
    </div>
  </div>

  <div v-else>
    <header class="topbar">
      <div class="brand">🧊 县域农贸冷库 · 批次链平台</div>
      <nav>
        <a :class="{on: ui.tab==='dashboard'}" @click="ui.tab='dashboard'">调度看板</a>
        <a :class="{on: ui.tab==='register'}" @click="ui.tab='register'">到货登记</a>
        <a :class="{on: ui.tab==='ops'}" @click="ui.tab='ops'">批次作业</a>
        <a :class="{on: ui.tab==='bills'}" @click="ui.tab='bills'">结算账单</a>
        <a :class="{on: ui.tab==='master'}" @click="ui.tab='master'">基础数据</a>
      </nav>
      <div class="who">
        {{ store.user.name }} <span class="tag blue">{{ roleName }}</span>
        <button class="ghost mini" @click="store.logout()">退出</button>
      </div>
    </header>
    <main class="main">
      <Dashboard v-if="ui.tab==='dashboard'" />
      <Register v-else-if="ui.tab==='register'" />
      <Ops v-else-if="ui.tab==='ops'" />
      <Bills v-else-if="ui.tab==='bills'" />
      <Master v-else-if="ui.tab==='master'" />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { store, api, toast } from './api'
import { ui } from './state'
import Dashboard from './views/Dashboard.vue'
import Register from './views/Register.vue'
import Ops from './views/Ops.vue'
import Bills from './views/Bills.vue'
import Master from './views/Master.vue'

const username = ref('admin')
const password = ref('admin123')

const roles = { ADMIN: '管理员', QC: '质检员', WAREHOUSE: '库管', FINANCE: '财务' }
const roleName = computed(() => roles[store.user?.role] || store.user?.role)

async function login() {
  try {
    const data = await api.post('/api/auth/login', { username: username.value, password: password.value })
    store.setAuth(data.token, data.user)
    toast('登录成功', true)
    ui.tab = 'dashboard'
  } catch (e) {
    toast(e.message)
  }
}
</script>

<style scoped>
.login-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #16513e 0%, #1f6f54 60%, #2d8f6c 100%); }
.login-card { background: #fff; padding: 34px 30px; border-radius: 14px; width: 380px;
  box-shadow: 0 18px 50px rgba(0,0,0,.3); }
.login-card h2 { font-size: 17px; color: #33424f; margin-bottom: 18px; }
.brand { font-weight: 700; color: var(--brand); font-size: 18px; margin-bottom: 10px; }
.topbar { display: flex; align-items: center; gap: 26px; background: #fff; padding: 0 22px;
  height: 58px; border-bottom: 1px solid var(--line); position: sticky; top: 0; z-index: 100; }
.topbar .brand { margin: 0; white-space: nowrap; }
.topbar nav { display: flex; gap: 4px; flex: 1; }
.topbar nav a { padding: 7px 14px; border-radius: 7px; color: #42505e; font-size: 14px; }
.topbar nav a:hover { background: #f0f4f2; }
.topbar nav a.on { background: var(--brand); color: #fff; }
.who { font-size: 13px; color: var(--muted); display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.main { padding: 20px 24px; max-width: 1500px; margin: 0 auto; }
</style>
