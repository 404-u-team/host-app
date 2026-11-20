<script setup>
import { reactive, ref, computed } from 'vue'
import { Eye, EyeOff, LogIn, Server } from 'lucide-vue-next'
import { api, login } from '../api'
import { useAction } from '../ui'
import Feedback from '../components/Feedback.vue'
import PasswordForm from '../components/PasswordForm.vue'

const props = defineProps({ needsPassword: Boolean, logoutError: [Error, Object] })
const emit = defineEmits(['signed-in', 'exit'])
const mode = ref('login')
const showPassword = ref(false)
const fields = reactive({ name: '', surname: '', email: '', password: '', code: '' })
const notice = ref('')
const { busy, error, run } = useAction()
const title = computed(() => props.needsPassword ? 'Смените временный пароль' : {
  login: 'Вход в систему', register: 'Создать аккаунт', verify: 'Подтвердите почту',
}[mode.value])

function switchMode(value) {
  error.value = null
  notice.value = ''
  showPassword.value = false
  fields.password = ''
  fields.code = ''
  mode.value = value
}

function submit() {
  run(async () => {
    if (mode.value === 'login') {
      await login(fields.email.trim(), fields.password)
      fields.password = ''
      emit('signed-in')
    } else if (mode.value === 'register') {
      const registered = await api('/auth/register', {
        public: true, method: 'POST',
        body: { name: fields.name.trim(), surname: fields.surname.trim(), email: fields.email.trim(), password: fields.password },
      })
      fields.email = registered.email
      if (registered.verificationRequired) {
        mode.value = 'verify'
      } else {
        await login(fields.email, fields.password)
        fields.password = ''
        emit('signed-in')
      }
    } else {
      await api('/auth/verify-email', {
        public: true, method: 'POST', body: { email: fields.email, code: fields.code },
      })
      await login(fields.email, fields.password)
      fields.password = ''
      emit('signed-in')
    }
  })
}

function resend() {
  run(async () => {
    await api('/auth/resend-email-code', {
      public: true, method: 'POST', body: { email: fields.email, password: fields.password },
    })
    notice.value = 'Новый код отправлен.'
  })
}
</script>

<template>
  <main class="auth-entry" aria-labelledby="auth-title">
    <div class="auth-form">
      <div class="brand auth-brand"><span class="brand-mark"><Server :size="20" aria-hidden="true" /></span>Host App</div>
      <h1 id="auth-title">{{ title }}</h1>
      <Feedback :error="logoutError" />
      <template v-if="needsPassword">
        <p class="auth-intro">Смените пароль, чтобы продолжить.</p>
        <PasswordForm @changed="emit('signed-in')" />
        <button type="button" class="text-button mt-6" @click="emit('exit')">Выйти</button>
      </template>
      <form v-else class="form-stack" @submit.prevent="submit">
        <template v-if="mode !== 'verify'">
          <div v-if="mode === 'register'" class="split-fields">
            <label>Имя<input v-model="fields.name" autocomplete="given-name" maxlength="255" required></label>
            <label>Фамилия<input v-model="fields.surname" autocomplete="family-name" maxlength="255" required></label>
          </div>
          <label>{{ mode === 'login' ? 'Логин' : 'Электронная почта' }}
            <input v-model="fields.email" :type="mode === 'login' ? 'text' : 'email'" autocomplete="username" maxlength="255" required>
          </label>
          <div class="form-field">
            <label for="auth-password">Пароль</label>
            <span class="password-field">
              <input id="auth-password" v-model="fields.password" :type="showPassword ? 'text' : 'password'"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" maxlength="255" required>
              <button type="button" class="button button-icon" :aria-label="showPassword ? 'Скрыть пароль' : 'Показать пароль'" @click="showPassword = !showPassword">
                <component :is="showPassword ? EyeOff : Eye" :size="18" />
              </button>
            </span>
          </div>
        </template>
        <template v-else>
          <p class="muted">Код отправлен на {{ fields.email }}.</p>
          <label>Код из письма<input v-model="fields.code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" pattern="[0-9]{6}" required></label>
        </template>
        <Feedback :error="error" :message="notice" />
        <button class="button button-primary" :disabled="busy"><LogIn :size="17" />
          {{ busy ? 'Подождите…' : { login: 'Войти', register: 'Зарегистрироваться', verify: 'Подтвердить' }[mode] }}
        </button>
        <div class="auth-links">
          <button type="button" class="text-button" :disabled="busy" @click="switchMode(mode === 'login' ? 'register' : 'login')">
            {{ mode === 'login' ? 'Создать аккаунт' : 'Вернуться ко входу' }}
          </button>
          <button v-if="mode === 'verify'" type="button" class="text-button" :disabled="busy" @click="resend">Отправить код снова</button>
        </div>
      </form>
    </div>
  </main>
</template>
