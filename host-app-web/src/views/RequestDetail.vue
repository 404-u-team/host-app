<script setup>
import { computed, reactive, ref } from 'vue'
import { Archive, ArchiveRestore, ChevronLeft, ChevronRight, Pencil, Pin, Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import { api } from '../api'
import { allowedStatuses, canEditRequest, date, priorities, statuses, types, useAction, useResource } from '../ui'
import Dialog from '../components/Dialog.vue'
import EmptyState from '../components/EmptyState.vue'
import Feedback from '../components/Feedback.vue'
import RequestForm from '../components/RequestForm.vue'

const props = defineProps({ id: String, account: Object, admin: Boolean, editor: Boolean, references: Array })
const path = `/api/requests/${props.id}`
const record = useResource(() => api(path))
const notes = useResource(() => api(path + '/notes'))
const task = useAction()
const dialog = ref(null)
const notice = ref('')
const pickedStatus = ref('')
const selectedNote = ref(null)
const noteFields = reactive({ body: '', pinned: false })
const row = record.data
const transitions = computed(() => row.value ? allowedStatuses(row.value.status) : [])
const mutable = computed(() => props.editor && row.value && !row.value.archived)
const canChangeNote = note => mutable.value && (props.admin || note.authorId === props.account.id)

function reload() { record.reload(); notes.reload() }
reload()

function openDialog(value) {
  task.error.value = null
  dialog.value = value
}

function changeStatus() {
  pickedStatus.value = transitions.value[0]
  openDialog('status')
}

function noteDialog(note = null) {
  selectedNote.value = note
  Object.assign(noteFields, { body: note?.body || '', pinned: note?.pinned || false })
  openDialog('note')
}

function run(succeeded, action) {
  task.run(async () => {
    await action()
    dialog.value = null
    notice.value = succeeded
  })
}

function archive() {
  const version = row.value.version
  const archived = row.value.archived
  run(archived ? 'Заявка восстановлена.' : 'Заявка отправлена в архив.', async () => {
    row.value = await (archived
      ? api(path + '/restore', { method: 'POST', body: { version } })
      : api(path + '?version=' + version, { method: 'DELETE' }))
  })
}

function remove() {
  run('', async () => {
    await api(path + '/permanent', { method: 'DELETE' })
    window.location.hash = '/requests'
  })
}

function applyStatus() {
  run('Статус заявки изменён.', async () => {
    const result = await api('/api/requests/bulk', {
      method: 'POST', body: { ids: [props.id], action: 'SET_STATUS', status: pickedStatus.value },
    })
    if (!result.succeeded.length) throw new Error(result.errors[0]?.message || 'Статус изменить не удалось.')
    if (row.value.type === 'CANCEL' && pickedStatus.value === 'COMPLETED') {
      window.location.hash = '/requests'
    } else reload()
  })
}

function saveNote() {
  run('Заметка сохранена.', async () => {
    const note = selectedNote.value
    await api(path + '/notes' + (note ? '/' + note.id : ''), {
      method: note ? 'PUT' : 'POST', body: { ...noteFields },
    })
    notes.reload()
  })
}

function deleteNote() {
  run('Заметка удалена.', async () => {
    await api(path + '/notes/' + selectedNote.value.id, { method: 'DELETE' })
    notes.reload()
  })
}
</script>

<template>
  <a href="#/requests" class="back-link"><ChevronLeft :size="17" /> К реестру заявок</a>
  <Feedback :error="record.error.value" :loading="record.loading.value && !row" />
  <button v-if="record.error.value" class="button" @click="reload"><RefreshCw :size="16" /> Повторить</button>
  <template v-if="row">
    <header class="page-head detail-head">
      <div><p class="eyebrow mono">{{ row.externalId }}</p><h1>{{ row.title }}</h1>
        <div class="detail-state"><span class="status" :data-status="row.status">{{ statuses[row.status] }}</span>
          <span>{{ types[row.type] }} сервера</span><span v-if="row.archived" class="muted">В архиве</span>
        </div>
      </div>
      <button v-if="editor && canEditRequest(row)" type="button" class="button button-primary" @click="openDialog('edit')"><Pencil :size="16" /> Изменить</button>
    </header>
    <Feedback v-if="!dialog" :error="task.error.value" :message="notice" />
    <div class="record-grid">
      <div class="record-main">
        <section class="ruled-section">
          <h2>Конфигурация</h2>
          <div class="metrics-strip record-metrics">
            <div class="metric"><span>CPU, ядра</span><strong class="mono">{{ row.cpuCores }}</strong></div>
            <div class="metric"><span>Память, ГБ</span><strong class="mono">{{ row.ramGb }}</strong></div>
            <div class="metric"><span>Диск, ГБ</span><strong class="mono">{{ row.diskGb }}</strong></div>
          </div>
          <dl class="record-props">
            <div><dt>Операционная система</dt><dd>{{ row.os }}</dd></div>
            <div><dt>Приоритет</dt><dd :class="{ 'text-accent': row.priority === 'HIGH' }">{{ priorities[row.priority] }}</dd></div>
            <div><dt>Срочность</dt><dd>{{ row.urgent ? 'Срочная' : 'Обычная' }}</dd></div>
          </dl>
        </section>
        <section v-if="row.description || row.tags?.length" class="ruled-section record-description">
          <h2>Описание</h2><p v-if="row.description">{{ row.description }}</p>
          <ul v-if="row.tags?.length" class="tag-list"><li v-for="tag in row.tags" :key="tag" class="mono">#{{ tag }}</li></ul>
        </section>
        <section class="ruled-section">
          <div class="line-heading"><h2>Заметки <span class="muted mono">{{ notes.data.value?.length ?? '' }}</span></h2>
            <button v-if="mutable" class="button" type="button" @click="noteDialog()"><Plus :size="17" /> Добавить</button>
          </div>
          <Feedback :error="notes.error.value" :loading="notes.loading.value && !notes.data.value" />
          <div v-if="notes.data.value?.length" class="note-list">
            <article v-for="note in notes.data.value" :key="note.id" class="note">
              <header><span v-if="note.pinned" class="note-pin"><Pin :size="15" /> Закреплена</span>
                <span>{{ note.authorId === account.id ? account.name + ' ' + account.surname : 'Автор ' + note.authorId.slice(0, 8) }}</span>
                <time :datetime="note.updatedAt">{{ date(note.updatedAt, true) }}</time>
              </header>
              <p>{{ note.body }}</p>
              <div v-if="canChangeNote(note)" class="flex flex-wrap gap-2">
                <button type="button" class="button button-quiet" @click="noteDialog(note)"><Pencil :size="15" /> Изменить</button>
                <button type="button" class="button button-quiet" @click="selectedNote = note; openDialog('delete-note')"><Trash2 :size="15" /> Удалить</button>
              </div>
            </article>
          </div>
          <EmptyState v-else-if="notes.data.value?.length === 0" title="Заметок пока нет" />
        </section>
      </div>
      <aside class="record-aside">
        <section class="ruled-section">
          <h2>Карточка</h2>
          <dl class="record-props vertical">
            <div><dt>UUID заявки</dt><dd class="mono break-all">{{ row.id }}</dd></div>
            <div><dt>Владелец</dt><dd :class="{ mono: row.ownerId !== account.id }" class="break-all">{{ row.ownerId === account.id ? 'Вы' : row.ownerId }}</dd></div>
            <div><dt>Создана</dt><dd>{{ date(row.createdAt, true) }}</dd></div>
            <div><dt>Изменена</dt><dd>{{ date(row.updatedAt, true) }}</dd></div>
            <div><dt>Версия</dt><dd class="mono">{{ row.version }}</dd></div>
          </dl>
          <a v-if="row.serverId" class="button linked-button" :href="'#/servers/' + row.serverId">Связанный сервер <ChevronRight :size="17" /></a>
        </section>
        <section v-if="editor" class="ruled-section">
          <h2>Действия</h2>
          <div class="stack-actions">
            <button v-if="admin && !row.archived && transitions.length" type="button" class="button" @click="changeStatus">Изменить статус</button>
            <button type="button" class="button" :disabled="task.busy.value" @click="row.archived ? archive() : openDialog('archive')">
              <component :is="row.archived ? ArchiveRestore : Archive" :size="17" />{{ row.archived ? 'Восстановить' : 'В архив' }}
            </button>
            <button v-if="admin && row.archived" type="button" class="button button-danger"
              :disabled="row.type === 'CREATE' && !!row.serverId" @click="openDialog('delete')"><Trash2 :size="17" /> Удалить навсегда</button>
            <p v-if="admin && row.archived && row.type === 'CREATE' && row.serverId" class="muted">Сначала закройте связанный сервер.</p>
            <button type="button" class="button button-quiet" :disabled="record.loading.value" @click="reload"><RefreshCw :size="16" /> Обновить карточку</button>
          </div>
        </section>
      </aside>
    </div>
    <Dialog v-if="dialog === 'edit'" title="Редактирование заявки" wide @close="dialog = null">
      <RequestForm :request="row" :references="references" @saved="row = $event; dialog = null; notice = 'Изменения сохранены.'" @close="dialog = null" />
    </Dialog>
    <Dialog v-else-if="dialog === 'note'" :title="selectedNote ? 'Редактирование заметки' : 'Новая заметка'" @close="dialog = null">
      <form class="form-stack" @submit.prevent="saveNote">
        <label>Текст<textarea v-model="noteFields.body" maxlength="2000" rows="5" required autofocus></textarea></label>
        <label class="check-label"><input v-model="noteFields.pinned" type="checkbox"> Закрепить заметку</label>
        <Feedback :error="task.error.value" />
        <div class="dialog-actions"><button class="button button-primary" :disabled="task.busy.value">Сохранить</button></div>
      </form>
    </Dialog>
    <Dialog v-else-if="dialog === 'status'" title="Изменение статуса" @close="dialog = null">
      <form class="form-stack" @submit.prevent="applyStatus">
        <label>Новый статус<select v-model="pickedStatus"><option v-for="key in transitions" :key="key" :value="key">{{ statuses[key] }}</option></select></label>
        <p v-if="row.type === 'CANCEL' && pickedStatus === 'COMPLETED'" class="muted">Выполнение отмены удалит сервер и связанные с ним заявки.</p>
        <Feedback :error="task.error.value" />
        <div class="dialog-actions"><button class="button button-primary" :disabled="task.busy.value">Изменить статус</button></div>
      </form>
    </Dialog>
    <Dialog v-else-if="['delete', 'archive', 'delete-note'].includes(dialog)" :title="{
      delete: 'Удалить заявку навсегда?', archive: 'Отправить заявку в архив?', 'delete-note': 'Удалить заметку?',
    }[dialog]" @close="dialog = null">
      <p class="muted">{{ dialog === 'archive' ? 'Её можно будет восстановить из архива.' : 'Отменить это действие невозможно.' }}</p>
      <Feedback :error="task.error.value" />
      <div class="dialog-actions">
        <button type="button" class="button" @click="dialog = null">Отмена</button>
        <button type="button" class="button" :class="dialog === 'archive' ? 'button-primary' : 'button-danger-solid'"
          :disabled="task.busy.value" @click="dialog === 'archive' ? archive() : dialog === 'delete' ? remove() : deleteNote()">
          {{ dialog === 'archive' ? 'Архивировать' : 'Удалить' }}
        </button>
      </div>
    </Dialog>
  </template>
</template>
