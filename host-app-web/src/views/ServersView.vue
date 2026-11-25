<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Pencil, RefreshCw, Search, Server, Trash2 } from 'lucide-vue-next'
import { api } from '../api'
import { date, useAction, useResource } from '../ui'
import EmptyState from '../components/EmptyState.vue'
import Feedback from '../components/Feedback.vue'
import Dialog from '../components/Dialog.vue'

const props = defineProps({ id: String, admin: Boolean, editor: Boolean })
const inventory = useResource(id => api(id ? `/servers/${id}` : '/servers'))
const refs = useResource(() => api('/api/references'))
const task = useAction()
const query = ref('')
const dialog = ref(null)
const model = reactive({})
const server = computed(() => props.id && !Array.isArray(inventory.data.value) ? inventory.data.value : null)
const rows = computed(() => (Array.isArray(inventory.data.value) ? inventory.data.value : []).filter(item =>
  [item.hostname, item.os, ...item.ipv4_addresses, ...item.ipv6_addresses].some(value =>
    value?.toLowerCase().includes(query.value.trim().toLowerCase()))))

function load(id = props.id) { inventory.reload(id) }
watch(() => props.id, id => {
  dialog.value = null
  inventory.data.value = null
  task.error.value = null
  load(id)
}, { immediate: true })

function open(kind) {
  const item = server.value
  Object.assign(model, {
    hostname: item.hostname, os: item.os || '', cpu_cores: item.cpu_cores, ram_gb: item.ram_gb, disk_gb: item.disk_gb,
    ipv4_addresses: item.ipv4_addresses.join('\n'), ipv6_addresses: item.ipv6_addresses.join('\n'),
  })
  if (kind === 'change') refs.reload()
  task.error.value = null
  dialog.value = kind
}

function submit() {
  task.run(async () => {
    const id = props.id
    const params = { cpu_cores: model.cpu_cores, ram_gb: model.ram_gb, disk_gb: model.disk_gb }
    if (dialog.value === 'edit') {
      const split = text => text.split(/[,\s]+/).filter(Boolean)
      inventory.data.value = await api(`/servers/${id}`, { method: 'PUT', body: {
        ...params, hostname: model.hostname.trim(), ipv4_addresses: split(model.ipv4_addresses),
        ipv6_addresses: split(model.ipv6_addresses),
      } })
    } else if (dialog.value === 'delete') {
      await api(`/servers/${id}`, { method: 'DELETE' })
      window.location.hash = '/servers'
    } else {
      const cancel = dialog.value === 'cancel'
      const request = await api(`/requests/server/${id}/${cancel ? 'cancel' : 'change'}`, {
        method: 'POST', body: cancel ? undefined : { ...params, os: model.os.trim() },
      })
      window.location.hash = `/requests/${request}`
    }
    dialog.value = null
  })
}

const dialogTitles = { edit: 'Параметры сервера', change: 'Запросить изменение сервера', cancel: 'Запросить отмену сервера?', delete: 'Удалить сервер?' }
</script>

<template>
  <a v-if="id" href="#/servers" class="back-link"><ChevronLeft :size="17" /> К списку серверов</a>
  <header class="page-head">
    <div><h1>{{ server?.hostname || 'Серверы' }}</h1>
      <p v-if="server?.os" class="page-subtitle">{{ server.os }}</p>
    </div>
    <button v-if="!id" class="button" type="button" :disabled="inventory.loading.value" @click="load()"><RefreshCw :size="17" /> Обновить</button>
    <button v-else-if="admin && server" class="button button-primary" type="button" @click="open('edit')"><Pencil :size="17" /> Изменить сервер</button>
  </header>
  <Feedback :error="inventory.error.value" :loading="inventory.loading.value && !inventory.data.value" />
  <button v-if="inventory.error.value" class="button" type="button" @click="load()">Повторить</button>
  <template v-if="!id">
    <div class="filter-bar"><label class="search-field"><Search :size="19" aria-hidden="true" /><span class="sr-only">Поиск серверов</span>
      <input v-model="query" placeholder="Имя, ОС или IP-адрес">
    </label></div>
    <div v-if="rows.length" class="table-scroll">
      <table class="ledger server-table"><thead><tr><th>Сервер</th><th>ОС</th><th class="resource-cell">Ресурсы</th><th class="network-cell">IP-адреса</th><th class="date-cell">Создан</th><th><span class="sr-only">Открыть</span></th></tr></thead>
        <tbody><tr v-for="item in rows" :key="item.id">
          <td class="cell-title"><a :href="'#/servers/' + item.id" class="inline-flex items-center gap-2"><Server :size="18" />{{ item.hostname }}</a></td>
          <td>{{ item.os || '—' }}</td><td class="mono resource-cell">{{ item.cpu_cores }} CPU · {{ item.ram_gb }} ГБ RAM<small>{{ item.disk_gb }} ГБ диска</small></td>
          <td class="mono network-cell">{{ item.ipv4_addresses[0] || item.ipv6_addresses[0] || '—' }}<small v-if="item.ipv4_addresses.length + item.ipv6_addresses.length > 1">и ещё {{ item.ipv4_addresses.length + item.ipv6_addresses.length - 1 }}</small></td>
          <td class="date-cell">{{ date(item.created_at) }}</td>
          <td class="cell-link"><a :href="'#/servers/' + item.id" :aria-label="'Открыть ' + item.hostname"><ChevronRight :size="18" /></a></td>
        </tr></tbody>
      </table>
    </div>
    <EmptyState v-else-if="Array.isArray(inventory.data.value) && !inventory.error.value"
      :title="query ? 'Серверы не найдены' : 'Серверов пока нет'" detail="Сервер появляется после выполнения заявки на создание." />
    <p v-if="rows.length" class="table-foot mono">Серверов {{ rows.length }}</p>
  </template>
  <template v-else-if="server">
    <div class="record-grid">
      <div class="record-main">
        <section class="ruled-section">
          <h2>Ресурсы</h2>
          <div class="metrics-strip record-metrics">
            <div class="metric"><span>CPU, ядра</span><strong class="mono">{{ server.cpu_cores }}</strong></div>
            <div class="metric"><span>Память, ГБ</span><strong class="mono">{{ server.ram_gb }}</strong></div>
            <div class="metric"><span>Диск, ГБ</span><strong class="mono">{{ server.disk_gb }}</strong></div>
          </div>
        </section>
        <section class="ruled-section">
          <h2>Сеть</h2>
          <dl class="record-props">
            <div v-for="family in ['ipv4', 'ipv6']" :key="family"><dt>{{ family === 'ipv4' ? 'IPv4' : 'IPv6' }}</dt>
              <dd v-if="server[family + '_addresses'].length"><ul class="mono break-all"><li v-for="address in server[family + '_addresses']" :key="address">{{ address }}</li></ul></dd>
              <dd v-else>—</dd>
            </div>
          </dl>
        </section>
      </div>
      <aside class="record-aside">
        <section class="ruled-section">
          <h2>Карточка сервера</h2>
          <dl class="record-props vertical">
            <div><dt>UUID</dt><dd class="mono break-all">{{ server.id }}</dd></div>
            <div><dt>Создан</dt><dd>{{ date(server.created_at, true) }}</dd></div>
            <div><dt>Изменён</dt><dd>{{ date(server.updated_at, true) }}</dd></div>
          </dl>
        </section>
        <section v-if="editor" class="ruled-section">
          <h2>Действия</h2>
          <div class="stack-actions">
            <button type="button" class="button" @click="open('change')">Запросить изменение</button>
            <button type="button" class="button" @click="open('cancel')">Запросить отмену</button>
            <button v-if="admin" type="button" class="button button-danger" @click="open('delete')"><Trash2 :size="17" /> Удалить сервер</button>
          </div>
        </section>
      </aside>
    </div>
    <Dialog v-if="dialog" :title="dialogTitles[dialog]" @close="dialog = null">
      <form class="form-stack" @submit.prevent="submit">
        <template v-if="dialog === 'change' || dialog === 'edit'">
          <label v-if="dialog === 'edit'">Имя сервера<input v-model="model.hostname" maxlength="255" required autofocus></label>
          <label v-else>Операционная система<input v-model="model.os" list="server-os" maxlength="255" required autofocus>
            <datalist id="server-os"><option v-for="ref in refs.data.value || []" :key="ref.id" :value="ref.name"></option></datalist>
          </label>
          <div class="split-fields triple">
            <label>CPU, ядра<input v-model.number="model.cpu_cores" type="number" min="1" required></label>
            <label>RAM, ГБ<input v-model.number="model.ram_gb" type="number" min="1" required></label>
            <label>Диск, ГБ<input v-model.number="model.disk_gb" type="number" min="1" required></label>
          </div>
          <template v-if="dialog === 'edit'">
            <label>Адреса IPv4<textarea v-model="model.ipv4_addresses" rows="2" placeholder="Один адрес на строку"></textarea></label>
            <label>Адреса IPv6<textarea v-model="model.ipv6_addresses" rows="2" placeholder="Один адрес на строку"></textarea></label>
          </template>
        </template>
        <p v-else class="muted">{{ dialog === 'delete' ? 'Будут удалены сервер и связанные с ним заявки.' : 'Сервер будет удалён после выполнения заявки администратором.' }}</p>
        <Feedback :error="task.error.value" />
        <div class="dialog-actions">
          <button type="button" class="button" @click="dialog = null">Отмена</button>
          <button class="button" :class="dialog === 'delete' ? 'button-danger-solid' : 'button-primary'" :disabled="task.busy.value">
            {{ { edit: 'Сохранить', change: 'Создать заявку', cancel: 'Создать заявку', delete: 'Удалить сервер' }[dialog] }}
          </button>
        </div>
      </form>
    </Dialog>
  </template>
</template>
