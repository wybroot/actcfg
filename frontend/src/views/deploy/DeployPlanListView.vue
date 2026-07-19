<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  api,
  type CreateDeployComponentPayload,
  type CreateDeployPlanPayload,
  type CreateDeployPlanVersionPayload,
  type DeployComponent,
  type DeployPlan,
  type DeployPlanVersion
} from '../../api/http'

const plans = ref<DeployPlan[]>([])
const versions = ref<DeployPlanVersion[]>([])
const components = ref<DeployComponent[]>([])
const selectedPlanId = ref<number>()
const selectedVersionId = ref<number>()
const loading = ref(false)
const versionLoading = ref(false)
const componentLoading = ref(false)
const error = ref('')
const versionError = ref('')
const componentError = ref('')
const planFormVisible = ref(false)
const versionFormVisible = ref(false)
const componentFormVisible = ref(false)

const planForm = reactive<CreateDeployPlanPayload>({
  planCode: '',
  planName: '',
  description: ''
})

const versionForm = reactive<CreateDeployPlanVersionPayload>({
  version: ''
})

const componentForm = reactive<CreateDeployComponentPayload>({
  componentName: '',
  componentType: 'APP',
  resourceVersionId: 1,
  deployOrder: 1,
  configTemplate: '',
  healthCheck: ''
})

const selectedPlan = computed(() => plans.value.find((item) => item.id === selectedPlanId.value))
const selectedVersion = computed(() => versions.value.find((item) => item.id === selectedVersionId.value))
const selectedVersionEditable = computed(() => selectedVersion.value?.editable ?? false)

onMounted(loadPlans)

async function loadPlans() {
  loading.value = true
  error.value = ''
  try {
    plans.value = await api.deployPlans()
    if (!selectedPlanId.value && plans.value.length > 0) {
      await selectPlan(plans.value[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '部署方案加载失败'
  } finally {
    loading.value = false
  }
}

async function selectPlan(plan: DeployPlan) {
  selectedPlanId.value = plan.id
  selectedVersionId.value = undefined
  components.value = []
  await loadVersions(plan.id)
}

async function loadVersions(planId: number) {
  versionLoading.value = true
  versionError.value = ''
  try {
    versions.value = await api.deployPlanVersions(planId)
    if (versions.value.length > 0) {
      await selectVersion(versions.value[0])
    } else {
      selectedVersionId.value = undefined
    }
  } catch (err) {
    versionError.value = err instanceof Error ? err.message : '版本加载失败'
  } finally {
    versionLoading.value = false
  }
}

async function selectVersion(version: DeployPlanVersion) {
  selectedVersionId.value = version.id
  await loadComponents(version.id)
}

async function loadComponents(versionId: number) {
  componentLoading.value = true
  componentError.value = ''
  try {
    components.value = await api.deployComponents(versionId)
  } catch (err) {
    componentError.value = err instanceof Error ? err.message : '组件加载失败'
  } finally {
    componentLoading.value = false
  }
}

function showPlanForm() {
  Object.assign(planForm, { planCode: '', planName: '', description: '' })
  planFormVisible.value = true
}

function showVersionForm() {
  Object.assign(versionForm, { version: '' })
  versionFormVisible.value = true
}

function showComponentForm() {
  Object.assign(componentForm, {
    componentName: '',
    componentType: 'APP',
    resourceVersionId: 1,
    deployOrder: 1,
    configTemplate: '',
    healthCheck: ''
  })
  componentFormVisible.value = true
}

async function submitPlan() {
  error.value = ''
  try {
    await api.createDeployPlan(planForm)
    planFormVisible.value = false
    await loadPlans()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '部署方案保存失败'
  }
}

async function submitVersion() {
  if (!selectedPlan.value) {
    return
  }
  versionError.value = ''
  try {
    await api.createDeployPlanVersion(selectedPlan.value.id, versionForm)
    versionFormVisible.value = false
    await loadVersions(selectedPlan.value.id)
  } catch (err) {
    versionError.value = err instanceof Error ? err.message : '方案版本保存失败'
  }
}

async function publishVersion(version: DeployPlanVersion) {
  versionError.value = ''
  try {
    await api.publishDeployPlanVersion(version.id)
    if (selectedPlan.value) {
      await loadVersions(selectedPlan.value.id)
    }
  } catch (err) {
    versionError.value = err instanceof Error ? err.message : '发布失败'
  }
}

async function submitComponent() {
  if (!selectedVersion.value) {
    return
  }
  componentError.value = ''
  try {
    await api.createDeployComponent(selectedVersion.value.id, componentForm)
    componentFormVisible.value = false
    await loadComponents(selectedVersion.value.id)
  } catch (err) {
    componentError.value = err instanceof Error ? err.message : '组件保存失败'
  }
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<template>
  <section class="page-card stack">
    <div class="page-header">
      <div>
        <h1>部署配置</h1>
        <p>维护部署方案、方案版本、组件编排、配置模板和健康检查规则。</p>
        <p>已发布版本只读，修改必须创建新版本。</p>
      </div>
      <div class="toolbar">
        <button class="button secondary" type="button" @click="loadPlans">刷新</button>
        <button class="button primary" type="button" @click="showPlanForm">新增部署方案</button>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>

    <form v-if="planFormVisible" class="panel form-grid" @submit.prevent="submitPlan">
      <label class="field">
        <span>方案编码</span>
        <input v-model.trim="planForm.planCode" maxlength="64" required placeholder="PLAN-002" />
      </label>
      <label class="field">
        <span>方案名称</span>
        <input v-model.trim="planForm.planName" maxlength="128" required placeholder="多环境部署方案" />
      </label>
      <label class="field field-wide">
        <span>说明</span>
        <textarea v-model.trim="planForm.description" maxlength="512" placeholder="部署范围、适用环境等说明" />
      </label>
      <div class="form-actions field-wide">
        <button class="button primary" type="submit">创建方案</button>
        <button class="button secondary" type="button" @click="planFormVisible = false">取消</button>
      </div>
    </form>

    <div v-if="loading" class="muted">部署方案加载中...</div>
    <div v-else-if="plans.length === 0" class="empty-state">暂无部署方案，请先创建一个方案。</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>编码</th>
          <th>名称</th>
          <th>当前版本</th>
          <th>状态</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="plan in plans" :key="plan.id" :class="{ selected: selectedPlanId === plan.id }">
          <td>{{ plan.planCode }}</td>
          <td>{{ plan.planName }}</td>
          <td>{{ plan.currentVersionId ?? '-' }}</td>
          <td><span class="badge" :class="plan.status.toLowerCase()">{{ plan.status }}</span></td>
          <td>{{ formatDate(plan.createdAt) }}</td>
          <td class="actions">
            <button class="link-button" type="button" @click="selectPlan(plan)">查看版本</button>
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selectedPlan" class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>{{ selectedPlan.planName }}</h2>
          <p class="muted">{{ selectedPlan.planCode }}</p>
        </div>
        <div class="toolbar">
          <button class="button secondary" type="button" @click="showVersionForm">新增版本</button>
        </div>
      </div>

      <p v-if="versionError" class="error-message">{{ versionError }}</p>

      <form v-if="versionFormVisible" class="form-grid" @submit.prevent="submitVersion">
        <label class="field">
          <span>版本号</span>
          <input v-model.trim="versionForm.version" maxlength="64" required placeholder="1.1.0" />
        </label>
        <div class="form-actions field-wide">
          <button class="button primary" type="submit">创建草稿版本</button>
          <button class="button secondary" type="button" @click="versionFormVisible = false">取消</button>
        </div>
      </form>

      <div v-if="versionLoading" class="muted">版本加载中...</div>
      <div v-else-if="versions.length === 0" class="empty-state">当前方案暂无版本。</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>版本号</th>
            <th>状态</th>
            <th>可编辑</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="version in versions" :key="version.id" :class="{ selected: selectedVersionId === version.id }">
            <td>{{ version.version }}</td>
            <td><span class="badge" :class="version.status.toLowerCase()">{{ version.status }}</span></td>
            <td>{{ version.editable ? '是' : '否' }}</td>
            <td>{{ formatDate(version.createdAt) }}</td>
            <td class="actions">
              <button class="link-button" type="button" @click="selectVersion(version)">查看组件</button>
              <button v-if="version.status === 'DRAFT'" class="link-button" type="button" @click="publishVersion(version)">发布</button>
            </td>
          </tr>
        </tbody>
      </table>

      <section v-if="selectedVersion" class="stack">
        <div class="page-header compact">
          <div>
            <h3>{{ selectedVersion.version }} 组件</h3>
            <p class="muted">{{ selectedVersion.editable ? '草稿版本，可编辑组件' : '已发布版本，只读' }}</p>
          </div>
          <div class="toolbar">
            <button v-if="selectedVersionEditable" class="button secondary" type="button" @click="showComponentForm">新增组件</button>
          </div>
        </div>

        <p v-if="componentError" class="error-message">{{ componentError }}</p>

        <form v-if="componentFormVisible && selectedVersionEditable" class="form-grid" @submit.prevent="submitComponent">
          <label class="field">
            <span>组件名称</span>
            <input v-model.trim="componentForm.componentName" maxlength="128" required placeholder="应用服务" />
          </label>
          <label class="field">
            <span>组件类型</span>
            <input v-model.trim="componentForm.componentType" maxlength="32" required placeholder="APP" />
          </label>
          <label class="field">
            <span>资源版本 ID</span>
            <input v-model.number="componentForm.resourceVersionId" type="number" min="1" required />
          </label>
          <label class="field">
            <span>部署顺序</span>
            <input v-model.number="componentForm.deployOrder" type="number" min="1" required />
          </label>
          <label class="field field-wide">
            <span>配置模板</span>
            <textarea v-model.trim="componentForm.configTemplate" maxlength="4096" placeholder="server.port=${app.port}" />
          </label>
          <label class="field field-wide">
            <span>健康检查</span>
            <textarea v-model.trim="componentForm.healthCheck" maxlength="1024" placeholder="GET /actuator/health" />
          </label>
          <div class="form-actions field-wide">
            <button class="button primary" type="submit">创建组件</button>
            <button class="button secondary" type="button" @click="componentFormVisible = false">取消</button>
          </div>
        </form>

        <div v-if="componentLoading" class="muted">组件加载中...</div>
        <div v-else-if="components.length === 0" class="empty-state">当前版本暂无组件。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>组件名称</th>
              <th>类型</th>
              <th>资源版本 ID</th>
              <th>部署顺序</th>
              <th>配置模板</th>
              <th>健康检查</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="component in components" :key="component.id">
              <td>{{ component.componentName }}</td>
              <td>{{ component.componentType }}</td>
              <td>{{ component.resourceVersionId }}</td>
              <td>{{ component.deployOrder }}</td>
              <td>{{ component.configTemplate || '-' }}</td>
              <td>{{ component.healthCheck || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </section>
  </section>
</template>
