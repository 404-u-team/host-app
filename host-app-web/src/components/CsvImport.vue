<script setup>
import { ref, watch } from 'vue'
import { FileCheck2, FileUp } from 'lucide-vue-next'
import { api } from '../api'
import { useAction } from '../ui'
import Feedback from './Feedback.vue'

const emit = defineEmits(['imported', 'close'])
const file = ref(null)
const mode = ref('CREATE_ONLY')
const preview = ref(null)
const { error, busy, run } = useAction()
watch([file, mode], () => { preview.value = null; error.value = null })

function form() {
  const data = new FormData()
  data.set('file', file.value)
  return data
}

function check() {
  run(async () => {
    preview.value = null
    const body = form()
    body.set('mode', mode.value)
    preview.value = await api('/api/requests/import/preview', { method: 'POST', body })
  })
}

function apply() {
  run(async () => {
    const body = form()
    body.set('previewId', preview.value.previewId)
    const result = await api('/api/requests/import/apply', { method: 'POST', body })
    emit('imported', result)
  })
}

function template() {
  const blob = new Blob(['externalId,title,description,referenceCode,cpuCores,ramGb,diskGb,urgent,tags\n'], { type: 'text/csv;charset=UTF-8' })
  const url = URL.createObjectURL(blob)
  const link = Object.assign(document.createElement('a'), { href: url, download: 'requests-template.csv' })
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 3000)
}
</script>

<template>
  <form class="form-stack" @submit.prevent="check">
    <label>Файл CSV<input type="file" accept=".csv,text/csv" :disabled="busy" required @change="file = $event.target.files[0] || null"></label>
    <label>Режим импорта<select v-model="mode" :disabled="busy">
      <option value="CREATE_ONLY">Только новые заявки</option><option value="UPSERT">Создать или обновить по коду заявки</option>
    </select></label>
    <button type="button" class="text-button justify-self-start" @click="template">Скачать шаблон CSV</button>
    <Feedback :error="error" />
    <section v-if="preview" class="import-result" aria-live="polite">
      <div class="flex flex-wrap gap-5">
        <span><strong class="mono">{{ preview.validRows }}</strong> корректных</span>
        <span><strong class="mono">{{ preview.invalidRows }}</strong> с ошибками</span>
      </div>
      <ul v-if="preview.errors.length" class="result-list">
        <li v-for="item in preview.errors" :key="item.row"><span class="mono">Строка {{ item.row }}</span> — {{ item.message }}</li>
      </ul>
      <p v-else class="muted">Проверка действительна 15 минут.</p>
    </section>
    <div class="dialog-actions">
      <button type="submit" class="button" :disabled="busy || !file"><FileCheck2 :size="17" /> Проверить файл</button>
      <button type="button" class="button button-primary" :disabled="busy || !preview?.previewId" @click="apply"><FileUp :size="17" /> Применить импорт</button>
    </div>
  </form>
</template>
