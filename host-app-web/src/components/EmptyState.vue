<script setup>
defineProps({ title: String, detail: String })
</script>

<template>
  <div class="empty">
    <p>{{ title }}</p>
    <span v-if="detail">{{ detail }}</span>
    <div v-if="$slots.default" class="mt-4"><slot /></div>
  </div>
</template>
