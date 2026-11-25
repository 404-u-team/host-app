<script setup>
import { ref } from 'vue'
import { ArrowUpRight, Code2, Download, KeyRound } from 'lucide-vue-next'
import { api, download } from '../api'
import { date, roles, useAction } from '../ui'
import Dialog from '../components/Dialog.vue'
import Feedback from '../components/Feedback.vue'
import PasswordForm from '../components/PasswordForm.vue'

defineProps({ account: Object, admin: Boolean })
const emit = defineEmits(['changed'])
const dialog = ref(null)
const notice = ref('')
const { error, busy, run } = useAction()

function openSwagger() {
  run(async () => {
    await api('/api/admin/docs/session', { method: 'POST' })
    window.location.assign('/swagger-ui.html')
  })
}
</script>

<template>
  <header class="page-head"><div><h1>Мой профиль</h1><p class="page-subtitle">{{ account.name }} {{ account.surname }}</p></div></header>
  <Feedback :message="notice" />
  <div class="record-grid">
    <section class="ruled-section">
      <h2>Учётная запись</h2>
      <dl class="record-props vertical">
        <div><dt>Логин</dt><dd>{{ account.login }}</dd></div>
        <div><dt>Роли</dt><dd>{{ account.roles.map(role => roles[role]).join(', ') }}</dd></div>
        <div><dt>Создана</dt><dd>{{ date(account.createdAt, true) }}</dd></div>
      </dl>
      <button type="button" class="button" @click="dialog = 'password'"><KeyRound :size="17" /> Сменить пароль</button>
    </section>
    <section v-if="admin" class="ruled-section">
      <h2>Инструменты API</h2>
      <div class="stack-actions">
        <button type="button" class="button" :disabled="busy" @click="openSwagger"><Code2 :size="18" /> Открыть Swagger</button>
        <button type="button" class="button" :disabled="busy" @click="run(() => download('/v3/api-docs', 'host-app-openapi.json'))"><Download :size="17" /> OpenAPI JSON</button>
        <a href="/actuator/health" class="button" target="_blank" rel="noopener">Готовность бэкенда <ArrowUpRight :size="16" /></a>
      </div>
      <Feedback v-if="!dialog" :error="error" />
    </section>
  </div>
  <Dialog v-if="dialog === 'password'" title="Сменить пароль" @close="dialog = null">
    <PasswordForm @changed="dialog = null; notice = 'Пароль изменён.'; emit('changed')" />
  </Dialog>
</template>
