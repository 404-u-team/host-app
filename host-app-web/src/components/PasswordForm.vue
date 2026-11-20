<script setup>
import { ref } from 'vue'
import { KeyRound } from 'lucide-vue-next'
import { changePassword } from '../api'
import { useAction } from '../ui'
import Feedback from './Feedback.vue'

const emit = defineEmits(['changed'])
const current = ref('')
const next = ref('')
const confirm = ref('')
const { busy, error, run } = useAction()

function submit() {
  run(async () => {
    if (next.value !== confirm.value) throw new Error('Новый пароль и подтверждение не совпадают.')
    if (new TextEncoder().encode(next.value).length > 72) throw new Error('Новый пароль должен занимать не более 72 байт.')
    await changePassword(current.value, next.value)
    current.value = next.value = confirm.value = ''
    emit('changed')
  })
}
</script>

<template>
  <form class="form-stack" @submit.prevent="submit">
    <label>Текущий пароль<input v-model="current" type="password" autocomplete="current-password" maxlength="255" required></label>
    <label>Новый пароль<input v-model="next" type="password" autocomplete="new-password" minlength="12" maxlength="72" required>
      <small class="muted">От 12 символов, до 72 байт</small>
    </label>
    <label>Повторите новый пароль<input v-model="confirm" type="password" autocomplete="new-password" minlength="12" maxlength="72" required></label>
    <Feedback :error="error" />
    <button type="submit" class="button button-primary" :disabled="busy"><KeyRound :size="17" />{{ busy ? 'Сохраняем…' : 'Сменить пароль' }}</button>
  </form>
</template>
