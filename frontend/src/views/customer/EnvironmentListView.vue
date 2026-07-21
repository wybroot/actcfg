<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api, type BindDeployPlanPayload, type Customer, type CustomerEnvironment, type EnvVariable, type SnapshotDetail } from '../../api/http'
import { useAuth } from '../../composables/useAuth'

const { hasRole } = useAuth()
const isSuperAdmin = computed(() => hasRole('SUPER_ADMIN'))

// ---- 客户选择 ----
const customers = ref<Customer[]>([])
const selectedCustomerId = ref<number>()

async function loadCustomers() {
  customers.value = await api.customers()
  if (!selectedCustomerId.value && customers.value.length > 0) {
    selectedCustomerId.value = customers.value[0].id
  }
}

watch(selectedCustomerId, (id) => { if (id) loadEnvironments(id) })

// ---- 环境列表 ----
const environments = ref<CustomerEnvironment[]>([])
const loading = ref(false)
const error = ref('')
const bindingError = ref('')
const bindFormVisible = ref(false)
const selectedEnvironmentId = ref<number>()
const bindForm = reactive<BindDeployPlanPayload>({ deployPlanVersionId: 1 })

const selectedEnvironment = computed(() => environments.value.find(e => e.id === selectedEnvironmentId.value))

async function loadEnvironments(customerId: number) {
  loading.value = true; error.value = ''
  try {
    environments.value = await api.customerEnvironments(customerId)
    if (!selectedEnvironmentId.value && environments.value.length > 0) {
      selectEnvironment(environments.value[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '环境加载失败'
  } finally { loading.value = false }
}

function selectEnvironment(env: CustomerEnvironment) {
  selectedEnvironmentId.value = env.id
  loadVariables(env.id)
  loadSnapshot(env.id)
}

// ---- 配置快照 ----
const snapshot = ref<import('../../api/http').SnapshotDetail | null>(null)
const snapshotError = ref('')
const editingComponentId = ref<number | null>(null)
const editingConfig = ref('')

async function loadSnapshot(environmentId: number) {
  snapshot.value = null
  snapshotError.value = ''
  try {
    snapshot.value = await api.environmentSnapshot(environmentId)
  } catch {
    snapshot.value = null // 未绑定则无快照
  }
}

function startEditConfig(componentId: number, current?: string) {
  editingComponentId.value = componentId
  editingConfig.value = current ?? ''
}

async function saveConfig() {
  if (!snapshot.value || editingComponentId.value === null) return
  try {
    await api.updateSnapshotComponentConfig(snapshot.value.snapshot.id, editingComponentId.value, editingConfig.value)
    editingComponentId.value = null
    await loadSnapshot(selectedEnvironmentId.value!)
  } catch (e: unknown) {
    snapshotError.value = e instanceof Error ? e.message : '保存失败'
  }
}

async function submitBind() {
  if (!selectedEnvironment.value) return
  bindingError.value = ''
  try {
    await api.bindEnvironmentDeployPlan(selectedEnvironment.value.id, bindForm)
    bindFormVisible.value = false
    await loadEnvironments(selectedCustomerId.value!)
  } catch (err) { bindingError.value = err instanceof Error ? err.message : '绑定失败' }
}

function formatVersionId(v?: number) { return v ? `版本 ID: ${v}` : '未绑定' }

// ---- 环境变量 ----
const variables = ref<EnvVariable[]>([])
const varLoading = ref(false)
const varError = ref('')
const varDialog = ref({ show: false, mode: 'create' as 'create'|'edit', varId: 0, loading: false, error: '',
  form: { key: '', value: '', sensitive: false } })
const cloneFromId = ref<number>()

async function loadVariables(environmentId: number) {
  varLoading.value = true; varError.value = ''
  try { variables.value = await api.envVariables(environmentId) }
  catch (e: unknown) { varError.value = e instanceof Error ? e.message : '加载变量失败' }
  finally { varLoading.value = false }
}

function openCreateVar() {
  varDialog.value = { show: true, mode: 'create', varId: 0, loading: false, error: '',
    form: { key: '', value: '', sensitive: false } }
}

function openEditVar(v: EnvVariable) {
  varDialog.value = { show: true, mode: 'edit', varId: v.id, loading: false, error: '',
    form: { key: v.variableKey, value: v.variableValue, sensitive: v.sensitive } }
}

async function submitVar() {
  varDialog.value.loading = true; varDialog.value.error = ''
  const envId = selectedEnvironmentId.value!
  try {
    const f = varDialog.value.form
    if (varDialog.value.mode === 'create') {
      await api.createVariable(envId, { key: f.key, value: f.value, sensitive: f.sensitive })
    } else {
      await api.updateVariable(envId, varDialog.value.varId, { value: f.value, sensitive: f.sensitive })
    }
    varDialog.value.show = false
    await loadVariables(envId)
  } catch (e: unknown) {
    varDialog.value.error = e instanceof Error ? e.message : '保存失败'
  } finally { varDialog.value.loading = false }
}

async function deleteVar(v: EnvVariable) {
  if (!confirm(`确认删除变量「${v.variableKey}」？`)) return
  try {
    await api.deleteVariable(selectedEnvironmentId.value!, v.id)
    await loadVariables(selectedEnvironmentId.value!)
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '删除失败') }
}

async function cloneVars() {
  if (!cloneFromId.value || !selectedEnvironmentId.value) return
  try {
    await api.cloneVariables(selectedEnvironmentId.value, cloneFromId.value)
    await loadVariables(selectedEnvironmentId.value)
    cloneFromId.value = undefined
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '克隆失败') }
}

async function rotateSecrets() {
  if (!window.confirm('将用当前活跃密钥重新加密所有敏感变量，确认轮换？')) return
  try {
    const count = await api.rotateSecrets()
    if (selectedEnvironmentId.value) await loadVariables(selectedEnvironmentId.value)
    alert(`密钥轮换完成，重新加密 ${count} 个敏感变量`)
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '轮换失败') }
}

onMounted(loadCustomers)
</script>

<template>
  <section class="page-card stack">
    <div class="page-header">
      <div>
        <h1>客户环境</h1>
        <p>选择客户后维护其环境，绑定部署方案版本并管理环境变量。</p>
      </div>
      <div class="toolbar">
        <select v-model="selectedCustomerId" class="select-input">
          <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.customerName }}</option>
        </select>
        <button class="button secondary" @click="selectedCustomerId && loadEnvironments(selectedCustomerId)">刷新</button>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="muted">加载中...</div>
    <div v-else-if="environments.length === 0" class="empty-state">暂无环境数据。</div>
    <table v-else class="data-table">
      <thead>
        <tr><th>环境名称</th><th>类型</th><th>绑定版本</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="env in environments" :key="env.id" :class="{ selected: selectedEnvironmentId === env.id }">
          <td>{{ env.environmentName }}</td>
          <td>{{ env.environmentType }}</td>
          <td>{{ formatVersionId(env.deployPlanVersionId) }}</td>
          <td><span class="badge" :class="env.status.toLowerCase()">{{ env.status }}</span></td>
          <td class="actions">
            <button class="link-button" @click="selectEnvironment(env)">查看变量</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 绑定方案 + 变量管理区 -->
    <section v-if="selectedEnvironment" class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>{{ selectedEnvironment.environmentName }}</h2>
          <p class="muted">{{ selectedEnvironment.environmentType }} · {{ formatVersionId(selectedEnvironment.deployPlanVersionId) }}</p>
        </div>
        <div class="toolbar">
          <button class="button primary" @click="bindFormVisible = !bindFormVisible">绑定部署方案版本</button>
        </div>
      </div>

      <p v-if="bindingError" class="error-message">{{ bindingError }}</p>
      <form v-if="bindFormVisible" class="form-grid" @submit.prevent="submitBind">
        <label class="field">
          <span>方案版本 ID</span>
          <input v-model.number="bindForm.deployPlanVersionId" type="number" min="1" required />
        </label>
        <div class="form-actions field-wide">
          <button class="button primary" type="submit">确认绑定</button>
          <button class="button secondary" type="button" @click="bindFormVisible = false">取消</button>
        </div>
      </form>

      <!-- 变量列表 -->
      <div class="var-toolbar">
        <h3>环境变量</h3>
        <div class="toolbar">
          <select v-model="cloneFromId" class="select-input" style="width:160px">
            <option value="" disabled>克隆来源环境</option>
            <option v-for="e in environments.filter(e => e.id !== selectedEnvironmentId)" :key="e.id" :value="e.id">
              {{ e.environmentName }}
            </option>
          </select>
          <button class="button secondary" :disabled="!cloneFromId" @click="cloneVars">克隆</button>
          <button v-if="isSuperAdmin" class="button secondary" @click="rotateSecrets">轮换密钥</button>
          <button class="button primary" @click="openCreateVar">+ 新增变量</button>
        </div>
      </div>

      <p v-if="varError" class="error-message">{{ varError }}</p>
      <p v-if="varLoading" class="muted">加载中...</p>
      <table v-else class="data-table">
        <thead>
          <tr><th>Key</th><th>Value</th><th>敏感</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="v in variables" :key="v.id">
            <td>{{ v.variableKey }}</td>
            <td>{{ v.sensitive ? (v.maskedValue ?? '******') : v.variableValue }}</td>
            <td>{{ v.sensitive ? '是' : '否' }}</td>
            <td class="actions">
              <button @click="openEditVar(v)">编辑</button>
              <button class="btn-danger" @click="deleteVar(v)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 配置快照面板 -->
    <section v-if="selectedEnvironment && snapshot" class="panel stack">
      <div class="var-toolbar">
        <h3>配置快照（绑定时自动生成，与源方案独立）</h3>
        <span class="muted" style="font-size:12px">{{ snapshot.snapshot.planName }} · v{{ snapshot.snapshot.versionLabel }}</span>
      </div>
      <p v-if="snapshotError" class="error">{{ snapshotError }}</p>
      <table class="data-table">
        <thead>
          <tr><th>组件名</th><th>类型</th><th>顺序</th><th>配置模板（可独立编辑）</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="c in snapshot.components" :key="c.id">
            <td>{{ c.componentName }}</td>
            <td>{{ c.componentType }}</td>
            <td>{{ c.deployOrder }}</td>
            <td>
              <template v-if="editingComponentId === c.id">
                <textarea v-model="editingConfig" style="width:100%;min-height:60px;font-size:12px;border:1px solid #d9d9d9;border-radius:4px;padding:4px" />
              </template>
              <span v-else class="config-preview">{{ c.configTemplate ?? '(无)' }}</span>
            </td>
            <td class="actions">
              <template v-if="editingComponentId === c.id">
                <button class="btn-primary" @click="saveConfig">保存</button>
                <button @click="editingComponentId = null">取消</button>
              </template>
              <button v-else @click="startEditConfig(c.id, c.configTemplate)">编辑配置</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 变量新增/编辑弹窗 -->
    <div v-if="varDialog.show" class="modal-overlay" @click.self="varDialog.show = false">
      <div class="modal">
        <h3>{{ varDialog.mode === 'create' ? '新增变量' : '编辑变量' }}</h3>
        <div class="field">
          <label>Key</label>
          <input v-model="varDialog.form.key" :disabled="varDialog.mode === 'edit'" placeholder="变量名称" />
        </div>
        <div class="field">
          <label>Value</label>
          <input v-model="varDialog.form.value" :type="varDialog.form.sensitive ? 'password' : 'text'" placeholder="变量值" />
        </div>
        <div class="field checkbox-row">
          <input type="checkbox" id="sensitive" v-model="varDialog.form.sensitive" />
          <label for="sensitive">敏感变量（展示时遮蔽）</label>
        </div>
        <p v-if="varDialog.error" class="error">{{ varDialog.error }}</p>
        <div class="modal-footer">
          <button @click="varDialog.show = false">取消</button>
          <button class="btn-primary" @click="submitVar" :disabled="varDialog.loading">
            {{ varDialog.loading ? '保存中...' : '保 存' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.toolbar { display: flex; gap: 8px; align-items: center; }
.select-input { padding: 5px 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; }
.var-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 16px 0 8px; }
.var-toolbar h3 { margin: 0; font-size: 15px; }
.btn-primary { background: #4361ee; color: #fff; border: none; border-radius: 4px; padding: 5px 14px; cursor: pointer; font-size: 13px; }
.btn-danger  { color: #e53e3e; background: none; border: 1px solid #e53e3e; border-radius: 4px; padding: 2px 8px; cursor: pointer; font-size: 12px; }
.actions { display: flex; gap: 6px; }
.actions button { font-size: 12px; padding: 2px 8px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; }
.error { color: #e53e3e; font-size: 13px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; border-radius: 8px; padding: 28px 32px; width: 400px; }
.modal h3 { margin: 0 0 20px; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 13px; color: #555; margin-bottom: 5px; }
.field input:not([type=checkbox]) { width: 100%; box-sizing: border-box; padding: 7px 10px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.field input:focus { outline: none; border-color: #4361ee; }
.field input:disabled { background: #f5f5f5; }
.checkbox-row { display: flex; align-items: center; gap: 8px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.modal-footer button { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; }
</style>

