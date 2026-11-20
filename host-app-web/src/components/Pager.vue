<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

const props = defineProps({ result: Object, page: Number, size: Number, disabled: Boolean })
const emit = defineEmits(['page', 'size'])
</script>

<template>
  <div class="pager" aria-label="Страницы">
    <span class="mono" aria-live="polite">
      {{ result?.totalElements ? page * size + 1 : 0 }}–{{ Math.min((page + 1) * size, result?.totalElements || 0) }}
      <span class="muted">из</span> {{ result?.totalElements ?? '—' }}
    </span>
    <div class="flex items-center gap-2">
      <label class="page-size"><span>На странице</span>
        <select :value="size" :disabled="disabled" @change="emit('size', Number($event.target.value))">
          <option v-for="option in [10, 20, 50, 100]" :key="option">{{ option }}</option>
        </select>
      </label>
      <button type="button" class="button button-icon" aria-label="Предыдущая страница"
        :disabled="disabled || page === 0" @click="emit('page', page - 1)"><ChevronLeft :size="18" /></button>
      <button type="button" class="button button-icon" aria-label="Следующая страница"
        :disabled="disabled || page + 1 >= (result?.totalPages || 0)" @click="emit('page', page + 1)"><ChevronRight :size="18" /></button>
    </div>
  </div>
</template>
