<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Archive, ArrowDownToLine, ArrowDownUp, ChevronRight, CloudUpload, Download, ListFilter, Plus, RefreshCw, Search } from 'lucide-vue-next'
import { api, download, withQuery } from '../api'
import { date, number, priorities, statuses, types, useAction, useResource } from '../ui'
import Pager from '../components/Pager.vue'
import Feedback from '../components/Feedback.vue'
import Dialog from '../components/Dialog.vue'
import EmptyState from '../components/EmptyState.vue'
import RequestForm from '../components/RequestForm.vue'
import CsvImport from '../components/CsvImport.vue'
import RequestDetail from './RequestDetail.vue'

const props = defineProps({ id: String, admin: Boolean, editor: Boolean, account: Object })
const defaults = () => ({
  q: '', status: '', referenceId: '', urgent: '', priority: '', tag: '', cpu: '', cpuOp: 'GTE',
  sort: 'createdAt', direction: 'desc',
})
const input = reactive(defaults())
const applied = ref(defaults())
const archive = ref('ACTIVE')
const page = ref(0)
const size = ref(20)
const advanced = ref(false)
const picked = ref([])
const dialog = ref(null)
const notice = ref('')
const bulk = reactive({ action: 'ARCHIVE', status: 'COMPLETED', tag: '', result: null })
const list = useResource(query => api(withQuery('/api/requests', query)))
const refs = useResource(() => api(withQuery('/api/references', props.admin ? { includeDisabled: true } : {})))
const summary = useResource(() => api('/api/analytics/summary'))
const task = useAction()

const rows = computed(() => list.data.value?.content || [])
const hasFilters = computed(() => Object.keys(input).some(key => input[key] !== defaults()[key]))
const allOnPage = computed(() => rows.value.length > 0 && rows.value.every(row => picked.value.includes(row.id)))
const query = computed(() => ({
  ...applied.value, cpuOp: applied.value.cpu === '' ? '' : applied.value.cpuOp, archive: archive.value,
}))
const metrics = computed(() => [
  { value: summary.data.value?.active, label: 'Активные', accent: false },
  { value: summary.data.value?.byStatus?.find(bucket => bucket.key === 'CREATED')?.count ?? (summary.data.value ? 0 : null), label: 'Новые', accent: true },
  { value: summary.data.value?.urgent, label: 'Срочные', accent: false },
  { value: summary.data.value?.archived, label: 'В архиве', accent: false },
])

watch(() => props.id, id => {
  if (!id) reload()
  if (!refs.data.value) refs.reload()
}, { immediate: true })
watch([query, page, size], () => { if (!props.id) loadList() })

function loadList() {
  picked.value = []
  list.reload({ ...query.value, page: page.value, size: size.value })
}

function reload() {
  loadList()
  summary.reload()
}

function changePage(value) { page.value = value }
function changeSize(value) { page.value = 0; size.value = value }
function setArchive(value) { page.value = 0; archive.value = value }
function applyFilters() { page.value = 0; applied.value = { ...input, q: input.q.trim(), tag: input.tag.trim() } }
function sortBy(key) {
  input.direction = input.sort === key && input.direction === 'asc' ? 'desc' : 'asc'
  input.sort = key
  applyFilters()
}
function sortState(key) {
  return applied.value.sort === key ? (applied.value.direction === 'asc' ? 'ascending' : 'descending') : undefined
}
function resetFilters() { Object.assign(input, defaults()); applyFilters() }

function togglePage() {
  picked.value = allOnPage.value ? [] : rows.value.map(row => row.id)
}

function exportRows(format) {
  task.run(() => format === 'csv'
    ? download(withQuery('/api/requests/export', query.value), 'server-requests.csv')
    : download('/requests/export', 'server-requests.xlsx'))
}

function openBulk() {
  Object.assign(bulk, { action: archive.value === 'ARCHIVED' ? 'RESTORE' : 'ARCHIVE', status: 'COMPLETED', tag: '', result: null })
  task.error.value = null
  dialog.value = 'bulk'
}

function applyBulk() {
  task.run(async () => {
    bulk.result = await api('/api/requests/bulk', {
      method: 'POST',
      body: {
        ids: picked.value, action: bulk.action,
        ...(bulk.action === 'SET_STATUS' ? { status: bulk.status } : {}),
        ...(['ADD_TAG', 'REMOVE_TAG'].includes(bulk.action) ? { tag: bulk.tag.trim() } : {}),
      },
    })
    reload()
  })
}

function created(row) {
  dialog.value = null
  window.location.hash = `/requests/${row.id}`
}

function imported(result) {
  dialog.value = null
  notice.value = `Импорт завершён. Создано: ${result.created}, обновлено: ${result.updated}.`
  reload()
}
</script>

<template>
  <RequestDetail v-if="id" :key="id" :id="id" :account="account" :admin="admin" :editor="editor" :references="refs.data.value || []" />
  <template v-else>
    <header class="page-head">
      <h1>Заявки</h1>
      <button v-if="editor" type="button" class="button button-primary" @click="dialog = 'new'"><Plus :size="19" /> Новая заявка</button>
    </header>
    <div class="metrics-strip" aria-label="Сводка заявок">
      <div v-for="metric in metrics" :key="metric.label" class="metric" :class="{ 'metric-accent': metric.accent }">
        <span>{{ metric.label }}</span><strong class="mono">{{ number(metric.value) }}</strong>
      </div>
    </div>
    <div class="section-bar">
      <nav class="tabs" aria-label="Область реестра">
        <button type="button" :aria-pressed="archive === 'ACTIVE'" @click="setArchive('ACTIVE')">Активные</button>
        <button type="button" :aria-pressed="archive === 'ARCHIVED'" @click="setArchive('ARCHIVED')"><Archive :size="16" /> Архив</button>
        <button type="button" :aria-pressed="archive === 'ALL'" @click="setArchive('ALL')">Все</button>
      </nav>
      <div class="flex flex-wrap items-center gap-2 section-tools">
        <button v-if="editor" type="button" class="button button-quiet" @click="dialog = 'import'"><CloudUpload :size="18" /> Импорт</button>
        <button type="button" class="button button-quiet" :disabled="task.busy.value" @click="exportRows('csv')"><Download :size="18" /> CSV</button>
        <button type="button" class="button button-quiet" title="Выгрузить все активные заявки" :disabled="task.busy.value" @click="exportRows('xlsx')"><ArrowDownToLine :size="17" /> XLSX</button>
        <button type="button" class="button button-icon button-quiet" aria-label="Обновить заявки" :disabled="list.loading.value" @click="reload"><RefreshCw :size="17" /></button>
      </div>
    </div>
    <form class="filter-bar" @submit.prevent="applyFilters">
      <div class="filter-main">
        <label class="search-field"><Search :size="19" aria-hidden="true" /><span class="sr-only">Поиск по заявкам</span>
          <input v-model="input.q" placeholder="Найти по названию, коду или ОС" maxlength="200">
        </label>
        <label class="select-compact"><span class="sr-only">Статус</span><select v-model="input.status" @change="applyFilters">
          <option value="">Все статусы</option><option v-for="(label, key) in statuses" :key="key" :value="key">{{ label }}</option>
        </select></label>
        <button type="button" class="button" :aria-expanded="advanced" @click="advanced = !advanced"><ListFilter :size="17" /> Фильтры</button>
        <button class="button" type="submit">Найти</button>
      </div>
      <div v-if="advanced" class="filter-expanded">
        <label>Операционная система<select v-model="input.referenceId">
          <option value="">Любая ОС</option><option v-for="item in refs.data.value || []" :key="item.id" :value="item.id">{{ item.name }}</option>
        </select></label>
        <label>Срочность<select v-model="input.urgent"><option value="">Любая</option><option value="true">Срочная</option><option value="false">Несрочная</option></select></label>
        <label>Приоритет<select v-model="input.priority"><option value="">Любой</option>
          <option v-for="(label, key) in priorities" :key="key" :value="key">{{ label }}</option>
        </select></label>
        <label>Тег<input v-model="input.tag" maxlength="48"></label>
        <label>Сравнение CPU<select v-model="input.cpuOp"><option value="GTE">Не менее</option><option value="GT">Больше</option>
          <option value="EQ">Равно</option><option value="LTE">Не более</option><option value="LT">Меньше</option></select></label>
        <label>Число ядер<input v-model.number="input.cpu" type="number" min="1"></label>
        <label>Сортировка<select v-model="input.sort"><option value="createdAt">Дата создания</option>
          <option value="updatedAt">Дата изменения</option><option value="cpu">CPU</option><option value="ram">RAM</option>
          <option value="title">Название</option><option value="externalId">Код</option><option value="priority">Приоритет</option></select></label>
        <label>Направление<select v-model="input.direction"><option value="desc">По убыванию</option><option value="asc">По возрастанию</option></select></label>
        <div class="filter-actions">
          <button class="button button-dark" type="submit">Применить</button>
          <button class="text-button" type="button" @click="resetFilters">Сбросить</button>
        </div>
      </div>
    </form>
    <Feedback v-if="dialog !== 'bulk'" :error="task.error.value" :message="notice" />
    <Feedback :error="list.error.value" :loading="list.loading.value && !list.data.value" />
    <div v-if="picked.length" class="selection-bar">
      <span><strong class="mono">{{ picked.length }}</strong> выбрано</span>
      <button type="button" class="button button-dark" @click="openBulk">Массовая операция</button>
      <button class="text-button" type="button" @click="picked = []">Снять выбор</button>
    </div>
    <div v-if="!list.error.value && rows.length" class="table-scroll" :aria-busy="list.loading.value">
      <table class="ledger request-table">
        <thead><tr>
          <th v-if="editor" class="cell-check"><input type="checkbox" aria-label="Выбрать заявки на странице" :checked="allOnPage" @change="togglePage"></th>
          <th :aria-sort="sortState('title')"><button type="button" @click="sortBy('title')">Заявка <ArrowDownUp :size="13" aria-hidden="true" /></button></th>
          <th class="scope-cell">Тип / ОС</th>
          <th class="resource-cell" :aria-sort="sortState('cpu')"><button type="button" @click="sortBy('cpu')">Ресурсы <ArrowDownUp :size="13" aria-hidden="true" /></button></th>
          <th>Статус</th>
          <th class="date-cell" :aria-sort="sortState('createdAt')"><button type="button" @click="sortBy('createdAt')">Создана <ArrowDownUp :size="13" aria-hidden="true" /></button></th>
          <th><span class="sr-only">Открыть</span></th>
        </tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id" :class="{ 'row-selected': picked.includes(row.id) }">
            <td v-if="editor" class="cell-check"><input v-model="picked" type="checkbox" :value="row.id" :aria-label="'Выбрать ' + row.title"></td>
            <td class="cell-title"><a :href="'#/requests/' + row.id">{{ row.title }}</a>
              <div class="row-meta"><span class="mono">{{ row.externalId }}</span><span v-if="row.urgent" class="urgent-mark">Срочная</span></div>
            </td>
            <td class="scope-cell"><span>{{ types[row.type] }}</span><small>{{ row.os }}</small></td>
            <td class="resource-cell"><span class="mono">{{ row.cpuCores }} <small>CPU</small> · {{ row.ramGb }} <small>ГБ RAM</small></span>
              <small>{{ row.diskGb }} ГБ диска</small></td>
            <td><span class="status" :data-status="row.status">{{ statuses[row.status] }}</span><small v-if="row.archived">В архиве</small></td>
            <td class="date-cell">{{ date(row.createdAt) }}</td>
            <td class="cell-link"><a :href="'#/requests/' + row.id" :aria-label="'Открыть ' + row.title"><ChevronRight :size="18" /></a></td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else-if="list.data.value && !list.loading.value && !list.error.value" :title="archive === 'ARCHIVED' ? 'В архиве пока пусто' : 'Заявки не найдены'"
      :detail="hasFilters ? 'Попробуйте изменить поисковый запрос или сбросить фильтры.' : undefined">
      <button v-if="hasFilters" type="button" class="button" @click="resetFilters">Сбросить фильтры</button>
      <button v-else-if="editor && archive === 'ACTIVE'" type="button" class="button" @click="dialog = 'new'"><Plus :size="16" /> Создать заявку</button>
    </EmptyState>
    <Pager v-if="list.data.value && !list.error.value" :result="list.data.value" :page="page" :size="size" :disabled="list.loading.value"
      @page="changePage" @size="changeSize" />
    <Dialog v-if="dialog === 'new'" title="Новая заявка" wide @close="dialog = null">
      <Feedback :error="refs.error.value" :loading="refs.loading.value" />
      <button v-if="refs.error.value" type="button" class="button" @click="refs.reload()">Повторить загрузку ОС</button>
      <RequestForm :references="refs.data.value || []" @saved="created" @close="dialog = null" />
    </Dialog>
    <Dialog v-if="dialog === 'import'" title="Импорт заявок" description="Формат файла — CSV в UTF-8." @close="dialog = null">
      <CsvImport @imported="imported" @close="dialog = null" />
    </Dialog>
    <Dialog v-if="dialog === 'bulk'" title="Массовая операция" @close="dialog = null">
      <form v-if="!bulk.result" class="form-stack" @submit.prevent="applyBulk">
        <label>Действие для {{ picked.length }} заявок<select v-model="bulk.action">
          <option value="ARCHIVE">Архивировать</option><option value="RESTORE">Восстановить</option>
          <option v-if="admin" value="SET_STATUS">Изменить статус</option><option value="ADD_TAG">Добавить тег</option><option value="REMOVE_TAG">Удалить тег</option>
        </select></label>
        <label v-if="bulk.action === 'SET_STATUS'">Новый статус<select v-model="bulk.status">
          <option v-for="key in ['COMPLETED', 'REJECTED', 'CANCELLED']" :key="key" :value="key">{{ statuses[key] }}</option>
        </select></label>
        <label v-if="['ADD_TAG', 'REMOVE_TAG'].includes(bulk.action)">Тег<input v-model="bulk.tag" maxlength="48" required></label>
        <Feedback :error="task.error.value" />
        <div class="dialog-actions"><button class="button button-primary" :disabled="task.busy.value">{{ task.busy.value ? 'Выполняем…' : 'Выполнить' }}</button></div>
      </form>
      <div v-else class="form-stack">
        <p aria-live="polite">Выполнено: <strong>{{ bulk.result.succeeded.length }}</strong>. Ошибок: <strong>{{ bulk.result.errors.length }}</strong>.</p>
        <ul v-if="bulk.result.errors.length" class="result-list"><li v-for="item in bulk.result.errors" :key="item.id">
          <span class="mono">{{ item.id }}</span><br>{{ item.message }}
        </li></ul>
        <div class="dialog-actions"><button type="button" class="button" @click="dialog = null">Готово</button></div>
      </div>
    </Dialog>
  </template>
</template>
