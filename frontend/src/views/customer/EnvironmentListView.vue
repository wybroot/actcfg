<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api, type BindDeployPlanPayload, type CustomerEnvironment } from '../../api/http'

const customerId = 1
const environments = ref<CustomerEnvironment[]>([])
const loading = ref(false)
const error = ref('')
const bindingError = ref('')
const bindFormVisible = ref(false)
const selectedEnvironmentId = ref<number>()
const bindForm = reactive<BindDeployPlanPayload>({
  deployPlanVersionId: 1
})

const selectedEnvironment = computed(() => environments.value.find((item) => item.id === selectedEnvironmentId.value))

onMounted(loadEnvironments)

async function loadEnvironments() {
  loading.value = true
  error.value = ''
  try {
    environments.value = await api.customerEnvironments(customerId)
    if (!selectedEnvironmentId.value && environments.value.length > 0) {
      selectEnvironment(environments.value[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '客户环境加载失败'
  } finally {
    loading.value = false
  }
}

function selectEnvironment(environment: CustomerEnvironment) {
  selectedEnvironmentId.value = environment.id
}

function showBindForm() {
  bindFormVisible.value = true
}

async function submitBind() {
  if (!selectedEnvironment.value) {
    return
  }
  bindingError.value = ''
  try {
    await api.bindEnvironmentDeployPlan(selectedEnvironment.value.id, bindForm)
    bindFormVisible.value = false
    await loadEnvironments()
  } catch (err) {
    bindingError.value = err instanceof Error ? err.message : '绑定失败'
  }
}

function formatVersionId(value?: number) {
  return value ? `版本 ID: ${value}` : '未绑定'
}
</script>

<template>
  <section class="page-card stack">
    <div class="page-header">
      <div>
        <h1>客户环境</h1>
        <p>维护客户开发、测试、生产、灾备等环境，绑定明确的部署方案版本。</p>
      </div>
      <div class="toolbar">
        <button class="button secondary" type="button" @click="loadEnvironments">刷新</button>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>

    <div v-if="loading" class="muted">客户环境加载中...</div>
    <div v-else-if="environments.length === 0" class="empty-state">暂无客户环境。</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>环境名称</th>
          <th>环境类型</th>
          <th>绑定版本</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="environment in environments" :key="environment.id" :class="{ selected: selectedEnvironmentId === environment.id }">
          <td>{{ environment.environmentName }}</td>
          <td>{{ environment.environmentType }}</td>
          <td>{{ formatVersionId(environment.deployPlanVersionId) }}</td>
          <td><span class="badge" :class="environment.status.toLowerCase()">{{ environment.status }}</span></td>
          <td class="actions">
            <button class="link-button" type="button" @click="selectEnvironment(environment)">查看 / 绑定</button>
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selectedEnvironment" class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>{{ selectedEnvironment.environmentName }}</h2>
          <p class="muted">{{ selectedEnvironment.environmentType }} · {{ formatVersionId(selectedEnvironment.deployPlanVersionId) }}</p>
        </div>
        <div class="toolbar">
          <button class="button primary" type="button" @click="showBindForm">绑定部署方案版本</button>
        </div>
      </div>

      <p class="muted">仅支持绑定已发布的部署方案版本。请先在部署配置页发布版本后再绑定。</p>
      <p v-if="bindingError" class="error-message">{{ bindingError }}</p>

      <form v-if="bindFormVisible" class="form-grid" @submit.prevent="submitBind">
        <label class="field">
          <span>部署方案版本 ID</span>
          <input v-model.number="bindForm.deployPlanVersionId" type="number" min="1" required />
        </label>
        <div class="form-actions field-wide">
          <button class="button primary" type="submit">确认绑定</button>
          <button class="button secondary" type="button" @click="bindFormVisible = false">取消</button>
        </div>
      </form>
    </section>
  </section>
</template>
