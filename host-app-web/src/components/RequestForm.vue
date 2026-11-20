<script setup>
import { computed, reactive } from 'vue'
import { Save } from 'lucide-vue-next'
import { api } from '../api'
import { useAction } from '../ui'
import Feedback from './Feedback.vue'

const props = defineProps({ request: Object, references: { type: Array, default: () => [] } })
const emit = defineEmits(['saved', 'close'])
const model = reactive({
  title: props.request?.title || '', externalId: props.request?.externalId || '',
  description: props.request?.description || '', referenceId: props.request?.referenceId || '',
  cpuCores: props.request?.cpuCores ?? 2, ramGb: props.request?.ramGb ?? 4, diskGb: props.request?.diskGb ?? 40,
  tags: props.request?.tags?.join(', ') || '', urgent: props.request?.urgent || false,
})
const available = computed(() => props.references.filter(ref => ref.enabled || ref.id === props.request?.referenceId))
const { error, busy, run } = useAction()

function submit() {
  run(async () => {
    const data = {
      ...model, title: model.title.trim(), externalId: model.externalId.trim() || null,
      tags: [...new Set(model.tags.split(/[,\s]+/).filter(Boolean))],
    }
    const saved = props.request
      ? await api(`/api/requests/${props.request.id}`, { method: 'PUT', body: { data, version: props.request.version } })
      : await api('/api/requests', { method: 'POST', body: data })
    emit('saved', saved)
  })
}
</script>

<template>
  <form class="form-stack" @submit.prevent="submit">
    <label>Название заявки<input v-model="model.title" maxlength="160" required autofocus placeholder="Например, сборочный сервер"></label>
    <div class="split-fields">
      <label>Операционная система
        <select v-model="model.referenceId" required>
          <option disabled value="">Выберите ОС</option>
          <option v-for="item in available" :key="item.id" :value="item.id">{{ item.name }}{{ item.enabled ? '' : ' · отключена' }}</option>
        </select>
      </label>
      <label>Код заявки<input v-model="model.externalId" maxlength="80" pattern="[a-zA-Z0-9_\-]{1,80}" placeholder="Сгенерируется автоматически"></label>
    </div>
    <label>Описание<textarea v-model="model.description" rows="3" maxlength="2000" placeholder="Для чего нужен сервер"></textarea></label>
    <div class="split-fields triple">
      <label>CPU, ядра<input v-model.number="model.cpuCores" type="number" min="1" max="10000" required></label>
      <label>RAM, ГБ<input v-model.number="model.ramGb" type="number" min="1" max="1000000" required></label>
      <label>Диск, ГБ<input v-model.number="model.diskGb" type="number" min="1" max="10000000" required></label>
    </div>
    <label>Теги<input v-model="model.tags" placeholder="Через запятую"></label>
    <label class="check-label"><input v-model="model.urgent" type="checkbox"> Срочная заявка</label>
    <Feedback :error="error" />
    <div class="dialog-actions">
      <button type="button" class="button" :disabled="busy" @click="emit('close')">Отмена</button>
      <button class="button button-primary" :disabled="busy || !available.length"><Save :size="16" /> {{ busy ? 'Сохраняем…' : request ? 'Сохранить' : 'Создать заявку' }}</button>
    </div>
  </form>
</template>
