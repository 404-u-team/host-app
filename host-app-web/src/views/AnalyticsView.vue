<script setup>
import { computed } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { api } from '../api'
import { number, priorities, statuses, useResource } from '../ui'
import EmptyState from '../components/EmptyState.vue'
import Feedback from '../components/Feedback.vue'

const summary = useResource(() => api('/api/analytics/summary'))
const report = summary.data
const groups = computed(() => report.value ? [
  { title: 'По статусу', values: report.value.byStatus, labels: statuses },
  { title: 'По операционной системе', values: report.value.byReference, labels: { UNASSIGNED: 'Без справочника' } },
  { title: 'По приоритету', values: report.value.byPriority, labels: priorities },
] : [])

summary.reload()
</script>

<template>
  <header class="page-head">
    <h1>Аналитика</h1>
    <button class="button" type="button" :disabled="summary.loading.value" @click="summary.reload()"><RefreshCw :size="17" /> Обновить</button>
  </header>
  <Feedback :error="summary.error.value" :loading="summary.loading.value && !report" />
  <div v-if="report" class="metrics-strip analytics-metrics">
    <div class="metric"><span>Всего</span><strong class="mono">{{ number(report.total) }}</strong></div>
    <div class="metric"><span>Активные</span><strong class="mono">{{ number(report.active) }}</strong></div>
    <div class="metric"><span>В архиве</span><strong class="mono">{{ number(report.archived) }}</strong></div>
    <div class="metric metric-accent"><span>Срочные</span><strong class="mono">{{ number(report.urgent) }}</strong></div>
    <div class="metric"><span>Средний CPU</span><strong class="mono">{{ number(report.averageCpu, 1) }}<small v-if="report.averageCpu != null"> ядер</small></strong></div>
  </div>
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
