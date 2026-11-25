import { shallowRef } from 'vue'

const storageKey = 'host-app.refresh'
export const session = shallowRef(null)
let refreshing
let loggingOut = false
let sessionGeneration = 0

export class ApiError extends Error {
  constructor(status, data) {
    super(data?.message || (status === 401 ? 'Войдите снова' : `Сервер вернул ошибку (${status})`))
    this.status = status
    this.fields = data?.fields || {}
    this.code = data?.code
    this.correlationId = data?.correlationId
  }
}

async function send(path, { method = 'GET', body, token, format } = {}) {
  const headers = { Accept: format ? '*/*' : 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  if (body != null && !(body instanceof FormData)) headers['Content-Type'] = 'application/json'
  let response
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body == null || body instanceof FormData ? body : JSON.stringify(body),
      credentials: 'same-origin',
    })
  } catch {
    throw new Error('Не удалось связаться с сервером. Проверьте подключение и повторите попытку.')
  }
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new ApiError(response.status, error)
  }
  if (response.status === 204) return undefined
  if (format === 'blob') return response.blob()
  if (format === 'text') return response.text()
  return response.json()
}

export function setSession(pair) {
  session.value = pair
  if (pair) sessionStorage.setItem(storageKey, pair.refreshToken)
  else sessionStorage.removeItem(storageKey)
}

async function refreshSession() {
  if (refreshing) return refreshing
  const refreshToken = session.value?.refreshToken || sessionStorage.getItem(storageKey)
  if (!refreshToken) return null
  const generation = sessionGeneration
  refreshing = send('/api/auth/refresh', { method: 'POST', body: { refreshToken } })
    .then(pair => {
      if (generation !== sessionGeneration) return null
      setSession(pair)
      return pair
    })
    .catch(error => {
      if (error.status !== 401) throw error
      if (generation === sessionGeneration) setSession(null)
      return null
    })
    .finally(() => { refreshing = undefined })
  return refreshing
}

export function restoreSession() {
  return refreshSession()
}

export async function api(path, options = {}) {
  if (options.public) return send(path, options)
  const pair = session.value
  try {
    return await send(path, { ...options, token: pair?.accessToken })
  } catch (error) {
    if (error.status !== 401 || !pair || loggingOut) throw error
    const updated = session.value && session.value !== pair ? session.value : await refreshSession()
    if (!updated) throw error
    return send(path, { ...options, token: updated.accessToken })
  }
}

export async function login(login, password) {
  const pair = await send('/api/auth/login', { method: 'POST', body: { login, password } })
  sessionGeneration++
  setSession(pair)
  return pair
}

export async function changePassword(currentPassword, newPassword) {
  const pair = await api('/api/auth/change-password', {
    method: 'POST', body: { currentPassword, newPassword },
  })
  sessionGeneration++
  setSession(pair)
  return pair
}

export async function logout() {
  loggingOut = true
  try {
    if (refreshing) await refreshing.catch(() => {})
    const refreshToken = session.value?.refreshToken || sessionStorage.getItem(storageKey)
    if (refreshToken) await send('/api/auth/logout', { method: 'POST', body: { refreshToken } })
  } finally {
    sessionGeneration++
    setSession(null)
    loggingOut = false
  }
}

export function withQuery(path, values) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(values)) {
    if (value !== '' && value != null) params.set(key, value)
  }
  return params.size ? `${path}?${params}` : path
}

export async function download(path, name) {
  const blob = await api(path, { format: 'blob' })
  const url = URL.createObjectURL(blob)
  const link = Object.assign(document.createElement('a'), { href: url, download: name })
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 3000)
}
