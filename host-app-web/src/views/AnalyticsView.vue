<script setup>
import { computed, ref, watch } from 'vue'
import { Database, RefreshCw } from 'lucide-vue-next'
import { api, withQuery } from '../api'
import { date, number, priorities, statuses, useAction, useResource } from '../ui'
import EmptyState from '../components/EmptyState.vue'
import Feedback from '../components/Feedback.vue'
import Dialog from '../components/Dialog.vue'

const props = defineProps({ admin: Boolean })
const source = ref('REAL')
const seed = ref(null)
const confirmSeed = ref(false)
const summary = useResource(() => api('/api/analytics/summary'))
const forecast = useResource(value => api(withQuery('/api/analytics/request-forecast', { source: value })))
const task = useAction()
const report = summary.data
const prediction = forecast.data
const chartMax = computed(() => Math.max(1, ...((prediction.value?.recentHistory || []).map(day => day.requests)), prediction.value?.expectedRequests || 0))
const groups = computed(() => report.value ? [
  { title: 'По статусу', values: report.value.byStatus, labels: statuses },
  { title: 'По операционной системе', values: report.value.byReference, labels: { UNASSIGNED: 'Без справочника' } },
  { title: 'По приоритету', values: report.value.byPriority, labels: priorities },
] : [])

summary.reload()
if (props.admin) {
  watch(source, value => {
    task.error.value = null
    prediction.value = null
    forecast.reload(value)
  }, { immediate: true })
}

function seedDemo() {
  task.run(async () => {
    seed.value = await api('/api/analytics/request-forecast/demo-data', { method: 'POST' })
    confirmSeed.value = false
    if (source.value === 'DEMO') forecast.reload('DEMO')
    else source.value = 'DEMO'
  })
}

function reload() {
  summary.reload()
  if (props.admin) forecast.reload(source.value)
}
</script>

<template>
  <header class="page-head">
    <h1>Аналитика</h1>
    <button class="button" type="button" :disabled="summary.loading.value || forecast.loading.value" @click="reload"><RefreshCw :size="17" /> Обновить</button>
  </header>
  <Feedback :error="summary.error.value" :loading="summary.loading.value && !report" />
  <div v-if="report" class="metrics-strip analytics-metrics">
    <div class="metric"><span>Всего</span><strong class="mono">{{ number(report.total) }}</strong></div>
    <div class="metric"><span>Активные</span><strong class="mono">{{ number(report.active) }}</strong></div>
    <div class="metric"><span>В архиве</span><strong class="mono">{{ number(report.archived) }}</strong></div>
    <div class="metric metric-accent"><span>Срочные</span><strong class="mono">{{ number(report.urgent) }}</strong></div>
    <div class="metric"><span>Средний CPU</span><strong class="mono">{{ number(report.averageCpu, 1) }}<small v-if="report.averageCpu != null"> ядер</small></strong></div>
  </div>
  <section v-if="admin" class="forecast">
    <header class="forecast-header">
      <div><h2>Прогноз на завтра</h2><p>Новые заявки на создание сервера</p></div>
      <div class="forecast-controls">
        <nav class="tabs" aria-label="История для прогноза">
          <button type="button" :aria-pressed="source === 'REAL'" @click="source = 'REAL'">Реальные</button>
          <button type="button" :aria-pressed="source === 'DEMO'" @click="source = 'DEMO'">Учебные</button>
        </nav>
        <button v-if="source === 'DEMO'" class="button" type="button" @click="confirmSeed = true"><Database :size="16" /> Заполнить историю</button>
      </div>
    </header>
    <Feedback :error="forecast.error.value" :loading="forecast.loading.value && !prediction"
      :message="source === 'DEMO' && seed ? 'Добавлено дней: ' + seed.inserted + ' из ' + seed.days : undefined" />
    <div v-if="prediction" class="forecast-body">
      <div class="forecast-number">
        <span class="mono">{{ date(prediction.date) }}</span>
        <strong class="mono">{{ prediction.expectedRequests ?? '—' }}</strong>
        <span v-if="prediction.status === 'OK'">ожидается заявок</span>
        <span v-else>Пока мало истории</span>
        <small>{{ prediction.historyDays }} полных дней · {{ prediction.timezone }}</small>
        <small v-if="prediction.status !== 'OK'">История должна охватывать хотя бы 35 дней.</small>
      </div>
      <div class="forecast-series">
        <p>Последние {{ prediction.windowDays }} дней <span class="legend-dot"></span> История <span v-if="prediction.status === 'OK'" class="legend-dot legend-forecast"></span><span v-if="prediction.status === 'OK'"> Прогноз</span></p>
        <div v-if="prediction.recentHistory.length" class="bar-chart" aria-label="Дневное количество заявок">
          <div v-for="day in prediction.recentHistory" :key="day.date" class="chart-item" :title="date(day.date) + ' · ' + day.requests">
            <i :style="{ height: Math.max(2, day.requests / chartMax * 100) + '%' }"></i><span>{{ Number(day.date.slice(-2)) }}</span>
          </div>
          <div v-if="prediction.status === 'OK'" class="chart-item chart-forecast" :title="date(prediction.date) + ' · прогноз ' + prediction.expectedRequests">
            <i :style="{ height: Math.max(2, prediction.expectedRequests / chartMax * 100) + '%' }"></i><span>{{ Number(prediction.date.slice(-2)) }}</span>
          </div>
        </div>
        <details v-if="prediction.recentHistory.length" class="history-details">
          <summary>Дневные значения</summary>
          <ul><li v-for="day in prediction.recentHistory" :key="day.date"><time :datetime="day.date">{{ date(day.date) }}</time><strong class="mono">{{ day.requests }}</strong></li></ul>
        </details>
        <div v-if="prediction.status === 'OK'" class="forecast-errors">
          <div><span>Средняя ошибка RNN</span><strong class="mono">{{ number(prediction.modelMae, 2) }}</strong></div>
          <div><span>Средняя ошибка прогноза по 7 дням</span><strong class="mono">{{ number(prediction.baselineMae, 2) }}</strong></div>
        </div>
      </div>
    </div>
    <Dialog v-if="confirmSeed" title="Подготовить учебную историю?" description="Сервер заполнит до 120 прошлых дней учебными значениями." @close="confirmSeed = false">
      <Feedback :error="task.error.value" />
      <div class="dialog-actions"><button type="button" class="button" @click="confirmSeed = false">Отмена</button>
        <button class="button button-primary" type="button" :disabled="task.busy.value" @click="seedDemo">{{ task.busy.value ? 'Заполняем…' : 'Заполнить историю' }}</button></div>
    </Dialog>
  </section>
  <div v-if="groups.length" class="breakdown">
    <section v-for="group in groups" :key="group.title" class="ruled-section">
      <h2>{{ group.title }}</h2>
      <dl v-if="group.values.length" class="breakdown-list">
        <div v-for="bucket in group.values" :key="bucket.key"><dt>{{ group.labels[bucket.key] || bucket.key }}</dt><dd class="mono">{{ bucket.count }}</dd></div>
      </dl>
      <EmptyState v-else title="Нет записей" />
    </section>
  </div>
</template>
