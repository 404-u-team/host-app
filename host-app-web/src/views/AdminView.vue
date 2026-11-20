<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { KeyRound, Pencil, Plus, RefreshCw, Search } from 'lucide-vue-next'
import { api, withQuery } from '../api'
import { date, roles, useAction, useResource } from '../ui'
import Dialog from '../components/Dialog.vue'
import EmptyState from '../components/EmptyState.vue'
import Feedback from '../components/Feedback.vue'
import Pager from '../components/Pager.vue'

const props = defineProps({ mode: String, account: Object })
const reference = props.mode === 'references'
const users = props.mode === 'users'
const audit = props.mode === 'audit'
const config = {
  references: { title: 'Справочник ОС', button: 'Добавить ОС' },
  users: { title: 'Пользователи', button: 'Новый пользователь' },
  audit: { title: 'Журнал аудита' },
}[props.mode]
const page = ref(0)
const size = ref(20)
const search = ref('')
const filter = reactive({ q: '', sort: 'happenedAt', direction: 'desc' })
const dialog = ref(null)
const editing = ref(null)
const event = ref(null)
const notice = ref('')
const model = reactive({})
const resource = useResource(() => api(reference
  ? '/api/references?includeDisabled=true'
  : withQuery('/api/admin/' + props.mode, { page: page.value, size: size.value, ...(audit ? filter : {}) })))
const { busy, error, run } = useAction()
const rows = computed(() => reference ? resource.data.value || [] : resource.data.value?.content || [])

watch([page, size, filter], () => resource.reload(), { immediate: true })
function changeSize(value) { size.value = value; page.value = 0 }
function applySearch() { page.value = 0; filter.q = search.value.trim() }

function open(item = null, kind = 'edit') {
  editing.value = item
  error.value = null
  notice.value = ''
  if (reference) {
    Object.assign(model, { code: item?.code || '', name: item?.name || '', enabled: item?.enabled ?? true })
  } else {
    Object.assign(model, {
      login: item?.login || '', name: item?.name || '', surname: item?.surname || '',
      roles: item ? [...item.roles] : ['EDITOR'], active: item?.active ?? true, temporaryPassword: '',
    })
  }
  dialog.value = kind
}

function toggleReference(item) {
  run(async () => {
    await api('/api/references/' + item.id, { method: 'PUT', body: { name: item.name, enabled: !item.enabled } })
    notice.value = item.enabled ? 'ОС отключена.' : 'ОС включена.'
    resource.reload()
  })
}

function submit() {
  run(async () => {
    const item = editing.value
    if (reference) {
      await api('/api/references' + (item ? '/' + item.id : ''), {
        method: item ? 'PUT' : 'POST',
        body: { name: model.name.trim(), enabled: model.enabled, ...(item ? {} : { code: model.code.trim() }) },
      })
    } else {
      const temporaryPassword = model.temporaryPassword || undefined
      if (temporaryPassword && new TextEncoder().encode(temporaryPassword).length > 72) {
        throw new Error('Временный пароль должен занимать не более 72 байт.')
      }
      if (dialog.value === 'reset') {
        await api('/api/admin/users/' + item.id + '/reset-password', { method: 'POST', body: { temporaryPassword } })
      } else {
        if (!model.roles.length) throw new Error('Выберите хотя бы одну роль.')
        const account = { name: model.name.trim(), surname: model.surname.trim(), login: model.login.trim(), roles: model.roles, active: model.active }
        await api('/api/admin/users' + (item ? '/' + item.id : ''), {
          method: item ? 'PUT' : 'POST',
          body: item ? account : { account, temporaryPassword },
        })
      }
      model.temporaryPassword = ''
    }
    notice.value = dialog.value === 'reset' ? 'Временный пароль установлен.' : 'Изменения сохранены.'
    dialog.value = null
    resource.reload()
  })
}

const eventNames = {
  LOGIN: 'Успешный вход', LOGIN_FAILED: 'Неудачный вход', LOGOUT: 'Выход',
  SERVER_CREATE_REQUEST: 'Заявка на создание', REQUEST_CREATED: 'Заявка создана', REQUEST_UPDATED: 'Заявка изменена',
  REQUEST_ARCHIVED: 'Заявка архивирована', REQUEST_RESTORED: 'Заявка восстановлена', REQUEST_DELETED: 'Заявка удалена',
  REQUEST_STATUS_CHANGED: 'Статус изменён', REQUEST_TAGS_CHANGED: 'Теги изменены', NOTE_CREATED: 'Добавлена заметка',
  NOTE_UPDATED: 'Заметка изменена', NOTE_DELETED: 'Заметка удалена', CSV_IMPORTED: 'Импорт CSV',
  SERVER_UPDATED: 'Сервер изменён', SERVER_DELETED: 'Сервер удалён', USER_REGISTERED: 'Регистрация',
  USER_CREATED: 'Пользователь создан', USER_UPDATED: 'Пользователь изменён', PASSWORD_RESET: 'Сброс пароля',
  PASSWORD_CHANGED: 'Пароль изменён', REFERENCE_CREATED: 'ОС добавлена', REFERENCE_UPDATED: 'ОС изменена',
  FORECAST_DEMO_SEEDED: 'Подготовлена учебная история',
}
</script>

<template>
  <header class="page-head">
    <h1>{{ config.title }}</h1>
    <div class="flex flex-wrap gap-2">
      <button type="button" class="button button-icon" aria-label="Обновить раздел" :disabled="resource.loading.value" @click="resource.reload()"><RefreshCw :size="18" /></button>
      <button v-if="config.button" type="button" class="button button-primary" @click="open()"><Plus :size="18" /> {{ config.button }}</button>
    </div>
  </header>
  <form v-if="audit" class="filter-bar" @submit.prevent="applySearch">
    <div class="filter-main">
      <label class="search-field"><Search :size="19" /><span class="sr-only">Поиск по аудиту</span><input v-model="search" maxlength="200" placeholder="Логин, событие, описание или correlation ID"></label>
      <label class="select-compact"><span class="sr-only">Поле сортировки</span><select v-model="filter.sort" @change="page = 0">
        <option value="happenedAt">По времени</option><option value="eventType">По событию</option><option value="actorLogin">По логину</option>
      </select></label>
      <label class="select-compact"><span class="sr-only">Направление сортировки</span><select v-model="filter.direction" @change="page = 0">
        <option value="desc">По убыванию</option><option value="asc">По возрастанию</option>
      </select></label>
      <button class="button">Найти</button>
    </div>
  </form>
  <Feedback :error="resource.error.value" :loading="resource.loading.value && !resource.data.value" />
  <Feedback v-if="!dialog" :error="error" :message="notice" />
  <div v-if="rows.length && !resource.error.value" class="table-scroll" :aria-busy="resource.loading.value">
    <table v-if="reference" class="ledger">
      <thead><tr><th>Операционная система</th><th>Код</th><th>Доступность</th><th>Действия</th></tr></thead>
      <tbody><tr v-for="item in rows" :key="item.id">
        <td class="cell-title"><strong>{{ item.name }}</strong></td><td class="mono">{{ item.code }}</td>
        <td><span class="status" :data-status="item.enabled ? 'COMPLETED' : 'CANCELLED'">{{ item.enabled ? 'Включена' : 'Отключена' }}</span></td>
        <td><div class="flex items-center gap-2">
          <button type="button" class="button button-icon" :aria-label="'Изменить ' + item.name" @click="open(item)"><Pencil :size="17" /></button>
          <button type="button" class="button" :disabled="busy" @click="toggleReference(item)">{{ item.enabled ? 'Отключить' : 'Включить' }}</button>
        </div></td>
      </tr></tbody>
    </table>
    <table v-else-if="users" class="ledger">
      <thead><tr><th>Учётная запись</th><th>Роли</th><th>Доступ</th><th>Создана</th><th>Действия</th></tr></thead>
      <tbody><tr v-for="item in rows" :key="item.id">
        <td class="cell-title"><strong>{{ item.name }} {{ item.surname }}</strong><small>{{ item.login }}</small></td>
        <td>{{ item.roles.map(role => roles[role]).join(', ') }}</td>
        <td><span class="status" :data-status="item.active ? 'COMPLETED' : 'CANCELLED'">{{ item.active ? 'Активен' : 'Отключён' }}</span>
          <small v-if="item.mustChangePassword">Ожидается смена пароля</small></td>
        <td class="date-cell">{{ date(item.createdAt) }}</td>
        <td><div class="flex items-center gap-2">
          <button type="button" class="button button-icon" :aria-label="'Изменить ' + item.login" @click="open(item)"><Pencil :size="17" /></button>
          <button type="button" class="button button-icon" :aria-label="'Сбросить пароль ' + item.login" title="Сбросить пароль" @click="open(item, 'reset')"><KeyRound :size="17" /></button>
        </div></td>
      </tr></tbody>
    </table>
    <table v-else class="ledger audit-table">
      <thead><tr><th>Время</th><th>Событие</th><th>Кто</th><th>Описание</th></tr></thead>
      <tbody><tr v-for="item in rows" :key="item.id">
        <td class="date-cell mono">{{ date(item.happenedAt, true) }}</td>
        <td class="cell-title"><button type="button" class="text-button" @click="event = item">{{ eventNames[item.eventType] || item.eventType }}</button>
          <small class="mono">{{ item.entityType }}</small></td>
        <td>{{ item.actorLogin || 'Система' }}</td><td>{{ item.description }}</td>
      </tr></tbody>
    </table>
  </div>
  <EmptyState v-else-if="resource.data.value && !resource.error.value && !resource.loading.value" title="Записей нет" />
  <Pager v-if="!reference && resource.data.value && !resource.error.value" :result="resource.data.value" :page="page"
    :size="size" :disabled="resource.loading.value" @page="page = $event" @size="changeSize" />
  <Dialog v-if="dialog" :title="dialog === 'reset' ? 'Сбросить пароль' : (editing ? 'Редактирование' : config.button)" @close="dialog = null">
    <form class="form-stack" @submit.prevent="submit">
      <template v-if="reference">
        <label>Название ОС<input v-model="model.name" maxlength="255" required autofocus></label>
        <label>Код<input v-model="model.code" maxlength="64" pattern="[a-zA-Z0-9_\-]{1,64}" :readonly="!!editing" required></label>
        <label class="check-label"><input v-model="model.enabled" type="checkbox"> Доступна для новых заявок</label>
      </template>
      <template v-else>
        <p v-if="dialog === 'reset'" class="muted">{{ editing.login }} должен будет сменить пароль при следующем входе.</p>
        <div v-else class="form-stack">
          <div class="split-fields">
            <label>Имя<input v-model="model.name" maxlength="255" required autofocus></label>
            <label>Фамилия<input v-model="model.surname" maxlength="255" required></label>
          </div>
          <label>Логин<input v-model="model.login" maxlength="255" required placeholder="user@example.org"></label>
          <fieldset class="form-stack compact"><legend>Роли</legend>
            <label v-for="(name, role) in roles" :key="role" class="check-label"><input v-model="model.roles" type="checkbox" :value="role">{{ name }}</label>
          </fieldset>
          <label class="check-label"><input v-model="model.active" type="checkbox" :disabled="editing?.id === account.id"> Учётная запись активна</label>
        </div>
        <label v-if="!editing || dialog === 'reset'">Временный пароль
          <input v-model="model.temporaryPassword" type="password" minlength="12" maxlength="72" autocomplete="new-password">
          <small class="muted">Можно оставить пустым, если сервер отправляет пароли по почте.</small>
        </label>
      </template>
      <Feedback :error="error" />
      <div class="dialog-actions"><button type="button" class="button" @click="dialog = null">Отмена</button>
        <button class="button button-primary" :disabled="busy">{{ busy ? 'Сохраняем…' : dialog === 'reset' ? 'Сбросить пароль' : 'Сохранить' }}</button></div>
    </form>
  </Dialog>
  <Dialog v-if="event" :title="eventNames[event.eventType] || event.eventType" @close="event = null">
    <p>{{ event.description }}</p>
    <dl class="record-props vertical">
      <div><dt>Время</dt><dd>{{ date(event.happenedAt, true) }}</dd></div>
      <div><dt>Пользователь</dt><dd>{{ event.actorLogin || 'Система' }}</dd></div>
      <div v-if="event.actorId"><dt>UUID пользователя</dt><dd class="mono break-all">{{ event.actorId }}</dd></div>
      <div><dt>Сущность</dt><dd class="mono">{{ event.entityType }} <span v-if="event.entityId" class="break-all">{{ event.entityId }}</span></dd></div>
      <div><dt>Correlation ID</dt><dd class="mono break-all">{{ event.correlationId || '—' }}</dd></div>
    </dl>
  </Dialog>
</template>
