import { reactive } from 'vue'

export const store = reactive({
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  setAuth(token, user) {
    this.token = token
    this.user = user
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
  },
  logout() {
    this.token = ''
    this.user = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
})

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (store.token) headers['X-Auth-Token'] = store.token
  const res = await fetch(path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new Error((data && data.error) || `请求失败 ${res.status}`)
  }
  return data
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b),
  put: (p, b) => request('PUT', p, b)
}

/** 后端枚举输出为 {code,label}，统一取 label */
export const lbl = (v) => (v == null ? '-' : typeof v === 'object' ? v.label : v)
export const code = (v) => (v == null ? '' : typeof v === 'object' ? v.code : v)

export function toast(msg, ok = false) {
  const el = document.createElement('div')
  el.className = 'toast ' + (ok ? 'ok' : 'err')
  el.textContent = msg
  document.body.appendChild(el)
  setTimeout(() => el.remove(), 3200)
}
