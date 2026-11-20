<script setup>
import { onMounted, ref, useId } from 'vue'
import { X } from 'lucide-vue-next'

defineProps({ title: String, wide: Boolean, description: String })
const emit = defineEmits(['close'])
const element = ref()
const titleId = useId()
const descriptionId = useId()
onMounted(() => element.value.showModal())
</script>

<template>
  <dialog ref="element" class="dialog" :class="{ 'dialog-wide': wide }" :aria-labelledby="titleId"
    :aria-describedby="description ? descriptionId : undefined" @cancel.prevent="emit('close')"
    @click.self="emit('close')">
    <div class="dialog-body">
      <header class="dialog-header">
        <div><h2 :id="titleId">{{ title }}</h2><p v-if="description" :id="descriptionId">{{ description }}</p></div>
        <button type="button" class="button button-icon" aria-label="Закрыть окно" @click="emit('close')"><X :size="20" /></button>
      </header>
      <slot />
    </div>
  </dialog>
</template>
