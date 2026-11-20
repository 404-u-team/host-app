import { ref, shallowRef } from 'vue'

export const statuses = {
  CREATED: 'Новая', APPROVED: 'Одобрена', COMPLETED: 'Выполнена',
  REJECTED: 'Отклонена', CANCELLED: 'Отменена',
}

export const priorities = { HIGH: 'Высокий', STANDARD: 'Обычный' }
export const types = { CREATE: 'Создание', CHANGE: 'Изменение', CANCEL: 'Отмена' }
export const roles = { VIEWER: 'Наблюдатель', EDITOR: 'Редактор', ADMIN: 'Администратор' }
export const priorityOf = user => ['ADMIN', 'EDITOR', 'VIEWER'].find(role => user.roles.includes(role))
export const allowedStatuses = status => ({
  CREATED: ['COMPLETED', 'REJECTED', 'CANCELLED'],
  APPROVED: ['COMPLETED', 'CANCELLED'],
}[status] || [])
export const canEditRequest = request =>
  !request.archived && request.status === 'CREATED' && request.type !== 'CANCEL'

export function date(value, withTime = false) {
  if (!value) return '—'
  const dayOnly = typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)
  const instant = new Date(dayOnly ? `${value}T00:00:00` : value)
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric', month: 'short', ...(withTime ? { hour: '2-digit', minute: '2-digit' } : { year: 'numeric' }),
  }).format(instant).replace(/ г\.$/, '')
}

export const number = (value, decimals = 0) => value == null
  ? '—'
  : new Intl.NumberFormat('ru-RU', { maximumFractionDigits: decimals }).format(value)

export function useResource(load) {
  const data = shallowRef(null)
  const loading = ref(false)
  const error = shallowRef(null)
  let revision = 0
  async function reload(...args) {
    const current = ++revision
    loading.value = true
    error.value = null
    try {
      const result = await load(...args)
      if (current === revision) data.value = result
      return result
    } catch (failure) {
      if (current === revision) error.value = failure
    } finally {
      if (current === revision) loading.value = false
    }
  }
  return { data, loading, error, reload }
}

export function useAction() {
  const busy = ref(false)
  const error = shallowRef(null)
  async function run(action) {
    if (busy.value) return
    busy.value = true
    error.value = null
    try {
      return await action()
    } catch (failure) {
      error.value = failure
    } finally {
      busy.value = false
    }
  }
  return { busy, error, run }
}
