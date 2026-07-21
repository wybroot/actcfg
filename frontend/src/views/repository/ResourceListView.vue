<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api, type CreateResourceVersionPayload, type HarborSyncPayload, type Resource, type ResourceSourceType, type ResourceStatus, type ResourceType, type ResourceVersion, type SourceRepository } from '../../api/http'

const resourceTypes: ResourceType[] = ['JAR', 'IMAGE', 'SQL', 'SCRIPT', 'CONFIG', 'PACKAGE']
const sourceTypes: ResourceSourceType[] = ['UPLOAD', 'HARBOR', 'NEXUS', 'MAVEN', 'INTERNAL_REPO']
const statuses: ResourceStatus[] = ['ENABLED', 'DISABLED']

const resources = ref<Resource[]>([])
const versions = ref<ResourceVersion[]>([])
const selectedResourceId = ref<number>()
const loading = ref(false)
const versionLoading = ref(false)
const error = ref('')
const versionError = ref('')
const resourceFormVisible = ref(false)
const editingResourceId = ref<number>()
const versionTab = ref<'json' | 'upload' | 'harbor'>('json')

const resourceForm = reactive({
  resourceCode: '',
  resourceName: '',
  resourceType: 'JAR' as ResourceType,
  sourceType: 'UPLOAD' as ResourceSourceType,
  status: 'ENABLED' as ResourceStatus,
  description: ''
})

const versionForm = reactive<CreateResourceVersionPayload>({
  version: '',
  externalUrl: '',
  imageRepository: '',
  imageTag: '',
  checksum: '',
  releaseNote: '',
  status: 'ENABLED'
})

// file upload state
const uploadForm = reactive({ version: '', releaseNote: '' })
const uploadFile = ref<File | null>(null)
const uploadLoading = ref(false)

// harbor sync state
const harborForm = reactive<HarborSyncPayload>({ sourceRepositoryId: undefined, project: '', repository: '', tag: '', version: '', releaseNote: '' })
const harborLoading = ref(false)

// 可选源仓库（仅启用的）
const sources = ref<SourceRepository[]>([])
const enabledSources = computed(() => sources.value.filter(s => s.status === 'ENABLED'))
async function loadSources() {
  try { sources.value = await api.sourceRepositories() } catch { /* 源仓库不可用不阻塞资源页 */ }
}

const selectedResource = computed(() => resources.value.find((item) => item.id === selectedResourceId.value))
const isEditingResource = computed(() => editingResourceId.value !== undefined)

onMounted(() => { loadResources(); loadSources() })

async function loadResources() {
  loading.value = true
  error.value = ''
  try {
    resources.value = await api.resources()
    if (!selectedResourceId.value && resources.value.length > 0) {
      await selectResource(resources.value[0])
    } else if (selectedResourceId.value && !resources.value.some((item) => item.id === selectedResourceId.value)) {
      selectedResourceId.value = undefined
      versions.value = []
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '资源列表加载失败'
  } finally {
    loading.value = false
  }
}

async function selectResource(resource: Resource) {
  selectedResourceId.value = resource.id
  await loadVersions(resource.id)
}

async function loadVersions(resourceId: number) {
  versionLoading.value = true
  versionError.value = ''
  try {
    versions.value = await api.resourceVersions(resourceId)
  } catch (err) {
    versionError.value = err instanceof Error ? err.message : '资源版本加载失败'
  } finally {
    versionLoading.value = false
  }
}

function showCreateForm() {
  editingResourceId.value = undefined
  Object.assign(resourceForm, {
    resourceCode: '',
    resourceName: '',
    resourceType: 'JAR',
    sourceType: 'UPLOAD',
    status: 'ENABLED',
    description: ''
  })
  resourceFormVisible.value = true
}

function showEditForm(resource: Resource) {
  editingResourceId.value = resource.id
  Object.assign(resourceForm, {
    resourceCode: resource.resourceCode,
    resourceName: resource.resourceName,
    resourceType: resource.resourceType,
    sourceType: resource.sourceType,
    status: resource.status,
    description: resource.description ?? ''
  })
  resourceFormVisible.value = true
}

async function submitResource() {
  error.value = ''
  try {
    if (isEditingResource.value && editingResourceId.value) {
      await api.updateResource(editingResourceId.value, {
        resourceName: resourceForm.resourceName,
        resourceType: resourceForm.resourceType,
        sourceType: resourceForm.sourceType,
        status: resourceForm.status,
        description: resourceForm.description
      })
    } else {
      await api.createResource({
        resourceCode: resourceForm.resourceCode,
        resourceName: resourceForm.resourceName,
        resourceType: resourceForm.resourceType,
        sourceType: resourceForm.sourceType,
        status: resourceForm.status,
        description: resourceForm.description
      })
    }
    resourceFormVisible.value = false
    await loadResources()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '资源保存失败'
  }
}

async function deleteResource(resource: Resource) {
  if (!window.confirm(`确认删除资源 ${resource.resourceName}？`)) {
    return
  }
  error.value = ''
  try {
    await api.deleteResource(resource.id)
    if (selectedResourceId.value === resource.id) {
      selectedResourceId.value = undefined
      versions.value = []
    }
    await loadResources()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '资源删除失败'
  }
}

async function submitVersion() {
  if (!selectedResource.value) {
    return
  }
  versionError.value = ''
  try {
    await api.createResourceVersion(selectedResource.value.id, { ...versionForm })
    Object.assign(versionForm, {
      version: '',
      externalUrl: '',
      imageRepository: '',
      imageTag: '',
      checksum: '',
      releaseNote: '',
      status: 'ENABLED'
    })
    await loadVersions(selectedResource.value.id)
  } catch (err) {
    versionError.value = err instanceof Error ? err.message : '版本保存失败'
  }
}

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  uploadFile.value = target.files?.[0] ?? null
}

async function submitUpload() {
  if (!selectedResource.value || !uploadFile.value) {
    versionError.value = '请选择文件'
    return
  }
  versionError.value = ''
  uploadLoading.value = true
  try {
    const fd = new FormData()
    fd.append('version', uploadForm.version)
    if (uploadForm.releaseNote) fd.append('releaseNote', uploadForm.releaseNote)
    fd.append('file', uploadFile.value)
    await api.uploadResourceVersion(selectedResource.value.id, fd)
    uploadForm.version = ''
    uploadForm.releaseNote = ''
    uploadFile.value = null
    await loadVersions(selectedResource.value.id)
  } catch (err) {
    versionError.value = err instanceof Error ? err.message : '文件上传失败'
  } finally {
    uploadLoading.value = false
  }
}

async function submitHarborSync() {
  if (!selectedResource.value) return
  versionError.value = ''
  harborLoading.value = true
  try {
    await api.harborSync(selectedResource.value.id, { ...harborForm })
    Object.assign(harborForm, { sourceRepositoryId: undefined, project: '', repository: '', tag: '', version: '', releaseNote: '' })
    await loadVersions(selectedResource.value.id)
  } catch (err) {
    versionError.value = err instanceof Error ? err.message : 'Harbor 同步失败'
  } finally {
    harborLoading.value = false
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
        <h1>产品仓库</h1>
        <p>管理 jar、镜像、SQL、脚本、配置模板等交付资源的索引和版本引用。</p>
      </div>
      <div class="toolbar">
        <button class="button secondary" type="button" @click="loadResources">刷新</button>
        <button class="button primary" type="button" @click="showCreateForm">新增资源</button>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>

    <form v-if="resourceFormVisible" class="panel form-grid" @submit.prevent="submitResource">
      <label class="field">
        <span>资源编码</span>
        <input v-model.trim="resourceForm.resourceCode" :disabled="isEditingResource" required maxlength="64" placeholder="RES-APP-001" />
      </label>
      <label class="field">
        <span>资源名称</span>
        <input v-model.trim="resourceForm.resourceName" required maxlength="128" placeholder="示例应用服务" />
      </label>
      <label class="field">
        <span>资源类型</span>
        <select v-model="resourceForm.resourceType">
          <option v-for="type in resourceTypes" :key="type" :value="type">{{ type }}</option>
        </select>
      </label>
      <label class="field">
        <span>资源来源</span>
        <select v-model="resourceForm.sourceType">
          <option v-for="type in sourceTypes" :key="type" :value="type">{{ type }}</option>
        </select>
      </label>
      <label class="field">
        <span>状态</span>
        <select v-model="resourceForm.status">
          <option v-for="status in statuses" :key="status" :value="status">{{ status }}</option>
        </select>
      </label>
      <label class="field field-wide">
        <span>说明</span>
        <textarea v-model.trim="resourceForm.description" maxlength="512" placeholder="资源用途、来源或交付说明" />
      </label>
      <div class="form-actions field-wide">
        <button class="button primary" type="submit">{{ isEditingResource ? '保存修改' : '创建资源' }}</button>
        <button class="button secondary" type="button" @click="resourceFormVisible = false">取消</button>
      </div>
    </form>

    <div v-if="loading" class="muted">资源加载中...</div>
    <div v-else-if="resources.length === 0" class="empty-state">暂无资源，请新增第一个交付资源。</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>编码</th>
          <th>名称</th>
          <th>类型</th>
          <th>来源</th>
          <th>状态</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="resource in resources" :key="resource.id" :class="{ selected: selectedResourceId === resource.id }">
          <td>{{ resource.resourceCode }}</td>
          <td>{{ resource.resourceName }}</td>
          <td>{{ resource.resourceType }}</td>
          <td>{{ resource.sourceType }}</td>
          <td><span class="badge" :class="resource.status.toLowerCase()">{{ resource.status }}</span></td>
          <td>{{ formatDate(resource.createdAt) }}</td>
          <td class="actions">
            <button class="link-button" type="button" @click="selectResource(resource)">查看版本</button>
            <button class="link-button" type="button" @click="showEditForm(resource)">编辑</button>
            <button class="link-button danger" type="button" @click="deleteResource(resource)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selectedResource" class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>{{ selectedResource.resourceName }} 版本</h2>
          <p class="muted">{{ selectedResource.resourceCode }} · {{ selectedResource.resourceType }} · {{ selectedResource.sourceType }}</p>
        </div>
      </div>

      <p v-if="versionError" class="error-message">{{ versionError }}</p>

      <div class="tabs">
        <button :class="['tab', { active: versionTab === 'json' }]" @click="versionTab = 'json'">手动登记</button>
        <button :class="['tab', { active: versionTab === 'upload' }]" @click="versionTab = 'upload'">文件上传</button>
        <button :class="['tab', { active: versionTab === 'harbor' }]" @click="versionTab = 'harbor'">Harbor 同步</button>
      </div>

      <!-- 手动登记 -->
      <form v-if="versionTab === 'json'" class="form-grid" @submit.prevent="submitVersion">
        <label class="field">
          <span>版本号</span>
          <input v-model.trim="versionForm.version" required maxlength="64" placeholder="1.0.0" />
        </label>
        <label class="field">
          <span>外部地址</span>
          <input v-model.trim="versionForm.externalUrl" maxlength="512" placeholder="internal://repo/app.jar" />
        </label>
        <label class="field">
          <span>镜像仓库</span>
          <input v-model.trim="versionForm.imageRepository" maxlength="256" placeholder="harbor.local/project/app" />
        </label>
        <label class="field">
          <span>镜像标签</span>
          <input v-model.trim="versionForm.imageTag" maxlength="128" placeholder="1.0.0" />
        </label>
        <label class="field">
          <span>checksum</span>
          <input v-model.trim="versionForm.checksum" maxlength="128" placeholder="sha256-placeholder" />
        </label>
        <label class="field">
          <span>状态</span>
          <select v-model="versionForm.status">
            <option v-for="status in statuses" :key="status" :value="status">{{ status }}</option>
          </select>
        </label>
        <label class="field field-wide">
          <span>发布说明</span>
          <textarea v-model.trim="versionForm.releaseNote" maxlength="1024" placeholder="版本变更说明" />
        </label>
        <div class="form-actions field-wide">
          <button class="button primary" type="submit">新增版本</button>
        </div>
      </form>

      <!-- 文件上传 -->
      <form v-else-if="versionTab === 'upload'" class="form-grid" @submit.prevent="submitUpload">
        <label class="field">
          <span>版本号</span>
          <input v-model.trim="uploadForm.version" required maxlength="64" placeholder="1.0.0" />
        </label>
        <label class="field">
          <span>选择文件</span>
          <input type="file" @change="onFileChange" />
        </label>
        <label class="field field-wide">
          <span>发布说明</span>
          <textarea v-model.trim="uploadForm.releaseNote" maxlength="1024" placeholder="版本变更说明" />
        </label>
        <div class="form-actions field-wide">
          <button class="button primary" type="submit" :disabled="uploadLoading">
            {{ uploadLoading ? '上传中...' : '上传并创建版本' }}
          </button>
          <span class="muted" v-if="uploadFile">已选择：{{ uploadFile.name }}</span>
        </div>
      </form>

      <!-- Harbor 同步 -->
      <form v-else class="form-grid" @submit.prevent="submitHarborSync">
        <label class="field field-wide">
          <span>源仓库（选填，留空用全局配置）</span>
          <select v-model="harborForm.sourceRepositoryId">
            <option :value="undefined">— 使用全局 app.harbor 配置 —</option>
            <option v-for="s in enabledSources" :key="s.id" :value="s.id">
              {{ s.repoName }} ({{ s.baseUrl }})
            </option>
          </select>
        </label>
        <label class="field">
          <span>Harbor 项目</span>
          <input v-model.trim="harborForm.project" required placeholder="library" />
        </label>
        <label class="field">
          <span>镜像仓库名</span>
          <input v-model.trim="harborForm.repository" required placeholder="myapp" />
        </label>
        <label class="field">
          <span>标签</span>
          <input v-model.trim="harborForm.tag" required placeholder="1.0.0" />
        </label>
        <label class="field">
          <span>版本号（选填，默认用标签）</span>
          <input v-model.trim="harborForm.version" placeholder="1.0.0" />
        </label>
        <label class="field field-wide">
          <span>发布说明</span>
          <textarea v-model.trim="harborForm.releaseNote" maxlength="1024" placeholder="版本变更说明" />
        </label>
        <div class="form-actions field-wide">
          <button class="button primary" type="submit" :disabled="harborLoading">
            {{ harborLoading ? '同步中...' : '从 Harbor 同步' }}
          </button>
        </div>
      </form>

      <div v-if="versionLoading" class="muted">版本加载中...</div>
      <div v-else-if="versions.length === 0" class="empty-state">当前资源暂无版本。</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>版本号</th>
            <th>外部地址</th>
            <th>镜像仓库</th>
            <th>镜像标签</th>
            <th>checksum</th>
            <th>状态</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="version in versions" :key="version.id">
            <td>{{ version.version }}</td>
            <td>{{ version.externalUrl || '-' }}</td>
            <td>{{ version.imageRepository || '-' }}</td>
            <td>{{ version.imageTag || '-' }}</td>
            <td>{{ version.checksum || '-' }}</td>
            <td><span class="badge" :class="version.status.toLowerCase()">{{ version.status }}</span></td>
            <td>{{ formatDate(version.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </section>
</template>

<style scoped>
.tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 1px solid #eee; }
.tab {
  padding: 8px 16px; border: none; background: none; cursor: pointer;
  font-size: 14px; color: #666; border-bottom: 2px solid transparent;
}
.tab.active { color: #4361ee; border-bottom-color: #4361ee; font-weight: 600; }
.tab:hover { color: #4361ee; }
</style>
