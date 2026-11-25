<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  Activity, ArrowUpRight, Boxes, ChartNoAxesCombined, ChevronRight, ClipboardList, Code2,
  ListChecks, LogOut, Menu, Server, Settings2, UsersRound,
} from 'lucide-vue-next'
import { api, logout, restoreSession, session } from './api'
import { priorityOf, roles, useAction } from './ui'
import AuthView from './views/AuthView.vue'
import RequestsView from './views/RequestsView.vue'
import ServersView from './views/ServersView.vue'
import AnalyticsView from './views/AnalyticsView.vue'
import AdminView from './views/AdminView.vue'
import AccountView from './views/AccountView.vue'
import Feedback from './components/Feedback.vue'

const account = ref(null)
const starting = ref(true)
const startError = ref(null)
const health = ref(null)
const menu = ref(false)
const locationHash = ref(window.location.hash)
const logoutTask = useAction()

const primary = [
  { key: 'requests', label: 'Заявки', icon: ClipboardList },
  { key: 'servers', label: 'Серверы', icon: Server },
  { key: 'analytics', label: 'Аналитика', icon: ChartNoAxesCombined },
]
const administration = [
  { key: 'references', label: 'Справочник ОС', icon: Boxes },
  { key: 'users', label: 'Пользователи', icon: UsersRound },
  { key: 'audit', label: 'Журнал аудита', icon: ListChecks },
]
const accountNav = { key: 'account', label: 'Мой профиль', icon: Settings2 }
const role = computed(() => account.value ? priorityOf(account.value) : null)
const admin = computed(() => role.value === 'ADMIN')
const editor = computed(() => admin.value || role.value === 'EDITOR')
const current = computed(() => {
  const [key, id] = locationHash.value.replace(/^#\/?/, '').split('/')
  const item = [...primary, ...(admin.value ? administration : []), accountNav].find(item => item.key === key) || primary[0]
  return { ...item, id: item.key === key && /^[a-f\d-]{36}$/i.test(id || '') ? id : null }
})
const initials = computed(() => [account.value?.name?.[0], account.value?.surname?.[0]].filter(Boolean).join('').toUpperCase())

function focusContent() {
  document.getElementById('main-content')?.focus()
}

function updateRoute() {
  locationHash.value = window.location.hash
  menu.value = false
  window.scrollTo(0, 0)
}
window.addEventListener('hashchange', updateRoute)

watch(session, value => { if (!value) account.value = null })
watch(current, route => { document.title = `${route.label} — Host App` })

async function updateAccount() {
  try {
    account.value = await api('/api/auth/me')
    startError.value = null
  } catch (error) {
    if (error.status === 401) account.value = null
    else startError.value = error
  }
}

async function refreshHealth() {
  try {
    health.value = (await api('/actuator/health', { public: true })).status === 'UP'
  } catch {
    health.value = false
  }
}

async function start() {
  starting.value = true
  startError.value = null
  try {
    if (session.value || await restoreSession()) await updateAccount()
  } catch (error) {
    startError.value = error
  } finally {
    starting.value = false
  }
}

function exit() {
  logoutTask.run(async () => {
    await logout()
    account.value = null
    window.location.hash = '/requests'
  })
}

onMounted(() => { start(); refreshHealth() })
</script>

<template>
  <div v-if="starting || startError" class="startup">
    <div class="brand"><span class="brand-mark"><Server :size="20" /></span> Host App</div>
    <Feedback :loading="starting" :error="startError" />
    <button v-if="startError" class="button" @click="start">Повторить</button>
  </div>
  <AuthView v-else-if="!account || account.mustChangePassword" :key="!!account"
    :needs-password="account?.mustChangePassword" :logout-error="logoutTask.error.value" @signed-in="updateAccount" @exit="exit" />
  <div v-else class="workspace">
    <a class="skip-link" href="#main-content" @click.prevent="focusContent">К содержимому</a>
    <aside class="sidebar" :class="{ 'sidebar-open': menu }">
      <a href="#/requests" class="brand sidebar-brand"><span class="brand-mark"><Server :size="20" /></span><span>Host App</span></a>
      <nav class="navigation" aria-label="Разделы">
        <p class="navigation-label">Рабочая область</p>
        <a v-for="item in primary" :key="item.key" :href="'#/' + item.key" class="nav-link"
          :aria-current="current.key === item.key ? 'page' : undefined">
          <component :is="item.icon" :size="19" aria-hidden="true" />{{ item.label }}
        </a>
        <template v-if="admin">
          <p class="navigation-label nav-group">Администрирование</p>
          <a v-for="item in administration" :key="item.key" :href="'#/' + item.key" class="nav-link"
            :aria-current="current.key === item.key ? 'page' : undefined">
            <component :is="item.icon" :size="19" aria-hidden="true" />{{ item.label }}
          </a>
        </template>
      </nav>
      <div class="sidebar-bottom">
        <a v-if="admin" href="#/account" class="sidebar-tools"><Code2 :size="16" /> Инструменты API <ArrowUpRight :size="14" /></a>
        <div class="sidebar-account">
          <a href="#/account" class="account-link" :aria-current="current.key === 'account' ? 'page' : undefined">
            <span class="avatar mono">{{ initials }}</span>
            <span><strong>{{ account.name }} {{ account.surname }}</strong><small>{{ roles[role] }}</small></span>
          </a>
          <button class="sidebar-logout" type="button" aria-label="Выйти" title="Выйти" :disabled="logoutTask.busy.value" @click="exit">
            <LogOut :size="17" />
          </button>
        </div>
      </div>
    </aside>
    <div class="workspace-body">
      <header class="topbar">
        <div class="breadcrumb">
          <button type="button" class="button button-icon mobile-menu" aria-label="Меню разделов" :aria-expanded="menu" @click="menu = !menu"><Menu :size="21" /></button>
          <a :href="'#/' + current.key">{{ current.label }}</a>
          <template v-if="current.id"><ChevronRight :size="14" /><span>Карточка</span></template>
        </div>
        <button type="button" class="monitor" :class="{ 'monitor-offline': health === false }" title="Проверить состояние API"
          @click="refreshHealth"><Activity :size="16" />{{ health == null ? 'Проверка API' : health ? 'API в сети' : 'API недоступен' }}</button>
      </header>
      <main id="main-content" class="main-content" tabindex="-1">
        <RequestsView v-if="current.key === 'requests'" :id="current.id" :admin="admin" :editor="editor" :account="account" />
        <ServersView v-else-if="current.key === 'servers'" :id="current.id" :admin="admin" :editor="editor" />
        <AnalyticsView v-else-if="current.key === 'analytics'" :admin="admin" />
        <AdminView v-else-if="admin && administration.some(item => item.key === current.key)" :key="current.key"
          :mode="current.key" :account="account" />
        <AccountView v-else :account="account" :admin="admin" @changed="updateAccount" />
      </main>
    </div>
  </div>
</template>
