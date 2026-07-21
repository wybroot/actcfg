<template>
  <section class="page-card">
    <div class="toolbar">
      <h2>用户管理</h2>
      <button class="btn-primary" @click="openCreate">+ 新增用户</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">加载中...</p>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>ID</th><th>用户名</th><th>显示名</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.id }}</td>
          <td>{{ u.username }}</td>
          <td>{{ u.displayName }}</td>
          <td><span class="role-tag" v-for="r in u.roles" :key="r">{{ r }}</span></td>
          <td><span :class="u.status === 'ENABLED' ? 'badge-ok' : 'badge-off'">{{ u.status }}</span></td>
          <td>{{ u.createdAt?.slice(0,10) }}</td>
          <td class="actions">
            <button @click="openEdit(u)">编辑</button>
            <button @click="openRoles(u)">角色</button>
            <button @click="openReset(u)">重置密码</button>
            <button class="btn-danger" @click="confirmDelete(u)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 新增/编辑弹窗 -->
    <div v-if="dialog.show" class="modal-overlay" @click.self="closeDialog">
      <div class="modal">
        <h3>{{ dialog.mode === 'create' ? '新增用户' : '编辑用户' }}</h3>
        <div class="field">
          <label>用户名</label>
          <input v-model="dialog.form.username" :disabled="dialog.mode === 'edit'" placeholder="用户名" />
        </div>
        <div class="field">
          <label>显示名</label>
          <input v-model="dialog.form.displayName" placeholder="显示名" />
        </div>
        <div class="field" v-if="dialog.mode === 'create'">
          <label>密码（≥6位）</label>
          <input v-model="dialog.form.password" type="password" placeholder="初始密码" />
        </div>
        <p v-if="dialog.error" class="error">{{ dialog.error }}</p>
        <div class="modal-footer">
          <button @click="closeDialog">取消</button>
          <button class="btn-primary" @click="submitDialog" :disabled="dialog.loading">
            {{ dialog.loading ? '保存中...' : '保 存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 角色分配弹窗 -->
    <div v-if="rolesDialog.show" class="modal-overlay" @click.self="rolesDialog.show = false">
      <div class="modal">
        <h3>分配角色 — {{ rolesDialog.username }}</h3>
        <div v-for="r in allRoles" :key="r.id" class="checkbox-row">
          <input type="checkbox" :id="'role_' + r.id" :value="r.id" v-model="rolesDialog.selected" />
          <label :for="'role_' + r.id">{{ r.roleName }} ({{ r.roleCode }})</label>
        </div>
        <p v-if="rolesDialog.error" class="error">{{ rolesDialog.error }}</p>
        <div class="modal-footer">
          <button @click="rolesDialog.show = false">取消</button>
          <button class="btn-primary" @click="submitRoles" :disabled="rolesDialog.loading">
            {{ rolesDialog.loading ? '保存中...' : '保 存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 重置密码弹窗 -->
    <div v-if="resetDialog.show" class="modal-overlay" @click.self="resetDialog.show = false">
      <div class="modal">
        <h3>重置密码 — {{ resetDialog.username }}</h3>
        <div class="field">
          <label>新密码（≥6位）</label>
          <input v-model="resetDialog.newPassword" type="password" placeholder="新密码" />
        </div>
        <p v-if="resetDialog.error" class="error">{{ resetDialog.error }}</p>
        <div class="modal-footer">
          <button @click="resetDialog.show = false">取消</button>
          <button class="btn-primary" @click="submitReset" :disabled="resetDialog.loading">
            {{ resetDialog.loading ? '重置中...' : '确认重置' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api, type UserVO, type RoleVO } from '../../api/http'

const users   = ref<UserVO[]>([])
const allRoles = ref<RoleVO[]>([])
const loading  = ref(false)
const error    = ref('')

const dialog = ref({
  show: false, mode: 'create' as 'create' | 'edit',
  userId: 0, loading: false, error: '',
  form: { username: '', displayName: '', password: '' }
})

const rolesDialog = ref({
  show: false, userId: 0, username: '',
  selected: [] as number[], loading: false, error: ''
})

const resetDialog = ref({
  show: false, userId: 0, username: '',
  newPassword: '', loading: false, error: ''
})

async function loadUsers() {
  loading.value = true; error.value = ''
  try { users.value = await api.users() }
  catch (e: unknown) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
}

async function loadRoles() {
  try { allRoles.value = await api.roles() } catch { /* ignore */ }
}

onMounted(() => { loadUsers(); loadRoles() })

function openCreate() {
  dialog.value = { show: true, mode: 'create', userId: 0, loading: false, error: '',
    form: { username: '', displayName: '', password: '' } }
}

function openEdit(u: UserVO) {
  dialog.value = { show: true, mode: 'edit', userId: u.id, loading: false, error: '',
    form: { username: u.username, displayName: u.displayName, password: '' } }
}

function closeDialog() { dialog.value.show = false }

async function submitDialog() {
  dialog.value.loading = true; dialog.value.error = ''
  try {
    if (dialog.value.mode === 'create') {
      await api.createUser({ username: dialog.value.form.username,
        displayName: dialog.value.form.displayName, password: dialog.value.form.password })
    } else {
      await api.updateUser(dialog.value.userId, { displayName: dialog.value.form.displayName })
    }
    closeDialog(); await loadUsers()
  } catch (e: unknown) {
    dialog.value.error = e instanceof Error ? e.message : '保存失败'
  } finally {
    dialog.value.loading = false
  }
}

function openRoles(u: UserVO) {
  const roleMap = new Map(allRoles.value.map(r => [r.roleCode, r.id]))
  rolesDialog.value = { show: true, userId: u.id, username: u.displayName,
    selected: u.roles.map(code => roleMap.get(code) ?? 0).filter(Boolean),
    loading: false, error: '' }
}

async function submitRoles() {
  rolesDialog.value.loading = true; rolesDialog.value.error = ''
  try {
    await api.assignRoles(rolesDialog.value.userId, { roleIds: rolesDialog.value.selected })
    rolesDialog.value.show = false; await loadUsers()
  } catch (e: unknown) {
    rolesDialog.value.error = e instanceof Error ? e.message : '保存失败'
  } finally { rolesDialog.value.loading = false }
}

function openReset(u: UserVO) {
  resetDialog.value = { show: true, userId: u.id, username: u.displayName,
    newPassword: '', loading: false, error: '' }
}

async function submitReset() {
  resetDialog.value.loading = true; resetDialog.value.error = ''
  try {
    await api.resetPassword(resetDialog.value.userId, { newPassword: resetDialog.value.newPassword })
    resetDialog.value.show = false
  } catch (e: unknown) {
    resetDialog.value.error = e instanceof Error ? e.message : '重置失败'
  } finally { resetDialog.value.loading = false }
}

async function confirmDelete(u: UserVO) {
  if (!confirm(`确认删除用户「${u.displayName}」？`)) return
  try { await api.deleteUser(u.id); await loadUsers() }
  catch (e: unknown) { alert(e instanceof Error ? e.message : '删除失败') }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h2 { margin: 0; font-size: 18px; }
.btn-primary { background: #4361ee; color: #fff; border: none; border-radius: 4px;
  padding: 6px 16px; cursor: pointer; font-size: 14px; }
.btn-danger { color: #e53e3e; background: none; border: 1px solid #e53e3e;
  border-radius: 4px; padding: 2px 8px; cursor: pointer; font-size: 12px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { border-bottom: 1px solid #eee; padding: 10px 12px; text-align: left; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.actions { display: flex; gap: 8px; }
.actions button { font-size: 12px; padding: 2px 8px; border: 1px solid #d9d9d9;
  border-radius: 4px; background: #fff; cursor: pointer; }
.actions button:hover { border-color: #4361ee; color: #4361ee; }
.role-tag { font-size: 11px; background: #e8f0fe; color: #4361ee;
  border-radius: 4px; padding: 1px 6px; margin-right: 4px; }
.badge-ok { color: #38a169; font-size: 12px; }
.badge-off { color: #999; font-size: 12px; }
.error { color: #e53e3e; font-size: 13px; margin-bottom: 8px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4);
  display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; border-radius: 8px; padding: 28px 32px; width: 400px; }
.modal h3 { margin: 0 0 20px; font-size: 16px; }
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.field input { width: 100%; box-sizing: border-box; padding: 7px 10px;
  border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.field input:focus { outline: none; border-color: #4361ee; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.modal-footer button { padding: 6px 16px; border: 1px solid #d9d9d9;
  border-radius: 4px; background: #fff; cursor: pointer; font-size: 14px; }
.checkbox-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; font-size: 14px; }
</style>

