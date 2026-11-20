<script setup>
import { CircleAlert, LoaderCircle } from 'lucide-vue-next'

defineProps({ error: [Error, Object], loading: Boolean, message: String })
</script>

<template>
  <div v-if="error" class="feedback feedback-error" role="alert">
    <CircleAlert :size="19" aria-hidden="true" />
    <div>
      <strong>{{ error.message }}</strong>
      <ul v-if="Object.keys(error.fields || {}).length">
        <li v-for="(value, field) in error.fields" :key="field">{{ field }} — {{ value }}</li>
      </ul>
      <small v-if="error.correlationId" class="mono">ID запроса {{ error.correlationId }}</small>
    </div>
  </div>
  <div v-else-if="loading" class="feedback" role="status">
    <LoaderCircle class="animate-spin" :size="18" aria-hidden="true" /> Загружаем…
  </div>
  <p v-else-if="message" class="feedback" role="status">{{ message }}</p>
</template>
