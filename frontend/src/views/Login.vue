<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="brand">
        <div class="logo">❄</div>
        <div>
          <div class="title">县域农贸批发冷库平台</div>
          <div class="subtitle">入库 · 质检 · 分拣 · 出库 · 结算 · 追溯 一体化</div>
        </div>
      </div>
      <el-form :model="form" @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                    prefix-icon="Lock" show-password @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider>测试账号</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc-tag" @click="fill(a)">
          {{ a.label }}：{{ a.u }} / {{ a.p }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

const accounts = [
  { label: '管理员', u: 'admin', p: 'admin123' },
  { label: '质检员', u: 'inspector', p: 'inspect123' },
  { label: '分拣员', u: 'sorter', p: 'sort123' },
  { label: '调度员', u: 'dispatcher', p: 'dispatch123' },
  { label: '财务', u: 'finance', p: 'finance123' },
  { label: '监管只读', u: 'viewer', p: 'view123' }
]

function fill(a) {
  form.username = a.u
  form.password = a.p
}

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号密码')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/api/auth/login', form)
    store.setLogin(res.token, res.user)
    ElMessage.success('欢迎，' + res.user.displayName)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6f54 0%, #2e8b6e 60%, #58a787 100%);
}
.login-card {
  width: 460px;
  border-radius: 14px;
}
.brand {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 22px;
}
.logo {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: #1f6f54;
  color: #fff;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.title {
  font-size: 19px;
  font-weight: 700;
  color: #1f2d3d;
}
.subtitle {
  font-size: 12px;
  color: #8794a3;
  margin-top: 4px;
}
.accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.acc-tag {
  cursor: pointer;
}
</style>
