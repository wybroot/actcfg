<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api, type CreatePackageBuildPayload, type ExecutionPlan, type PackageBuild, type PackageDownloadInfo, type PackageManifest } from '../../api/http'

const packages = ref<PackageBuild[]>([])
const selectedPackageId = ref<number>()
const manifest = ref<PackageManifest>()
const downloadInfo = ref<PackageDownloadInfo>()
const executionPlan = ref<ExecutionPlan>()
const loading = ref(false)
const manifestLoading = ref(false)
const error = ref('')
const manifestError = ref('')
const actionMessage = ref('')
const formVisible = ref(false)

const packageForm = reactive<CreatePackageBuildPayload>({
  customerId: 1,
  environmentId: 1,
  deployPlanVersionId: 1,
  packageVersion: '',
  remark: ''
})

const selectedPackage = computed(() => packages.value.find((item) => item.id === selectedPackageId.value))
const formattedManifest = computed(() => {
  if (!manifest.value?.manifestJson) {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(manifest.value.manifestJson), null, 2)
  } catch {
    return manifest.value.manifestJson
  }
})

onMounted(loadPackages)

async function loadPackages() {
  loading.value = true
  error.value = ''
  try {
    packages.value = await api.packages()
    if (!selectedPackageId.value && packages.value.length > 0) {
      await selectPackage(packages.value[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '部署包加载失败'
  } finally {
    loading.value = false
  }
}

async function selectPackage(packageBuild: PackageBuild) {
  selectedPackageId.value = packageBuild.id
  await loadManifest(packageBuild.id)
}

async function loadManifest(packageBuildId: number) {
  manifestLoading.value = true
  manifestError.value = ''
  manifest.value = undefined
  downloadInfo.value = undefined
  try {
    manifest.value = await api.packageManifest(packageBuildId)
  } catch (err) {
    manifestError.value = err instanceof Error ? err.message : 'manifest 加载失败'
  } finally {
    manifestLoading.value = false
  }
}

function showBuildForm() {
  Object.assign(packageForm, {
    customerId: 1,
    environmentId: 1,
    deployPlanVersionId: 1,
    packageVersion: '',
    remark: ''
  })
  formVisible.value = true
}

async function submitBuild() {
  error.value = ''
  actionMessage.value = ''
  try {
    const created = await api.createPackageBuild(packageForm)
    formVisible.value = false
    await loadPackages()
    await selectPackage(created)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '部署包生成失败'
  }
}

async function refreshStatus(packageBuild: PackageBuild) {
  error.value = ''
  actionMessage.value = ''
  try {
    const status = await api.packageStatus(packageBuild.id)
    packageBuild.buildStatus = status
    actionMessage.value = `${packageBuild.packageCode} 当前状态：${status}`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '状态查询失败'
  }
}

async function loadDownloadInfo(packageBuild: PackageBuild) {
  error.value = ''
  actionMessage.value = ''
  try {
    await selectPackage(packageBuild)
    downloadInfo.value = await api.packageDownloadInfo(packageBuild.id)
    actionMessage.value = `下载信息已生成：${downloadInfo.value.filePath}`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '下载信息加载失败'
  }
}

async function loadExecutionPlan(packageBuild: PackageBuild) {
  error.value = ''
  actionMessage.value = ''
  executionPlan.value = undefined
  try {
    await selectPackage(packageBuild)
    executionPlan.value = await api.packageExecutionPlan(packageBuild.id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '执行计划加载失败'
  }
}

async function deletePackage(packageBuild: PackageBuild) {
  if (!window.confirm(`确认删除部署包 ${packageBuild.packageCode}？`)) {
    return
  }
  error.value = ''
  actionMessage.value = ''
  try {
    await api.deletePackageBuild(packageBuild.id)
    if (selectedPackageId.value === packageBuild.id) {
      selectedPackageId.value = undefined
      manifest.value = undefined
      downloadInfo.value = undefined
    }
    await loadPackages()
    actionMessage.value = `${packageBuild.packageCode} 已删除`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
  }
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function shortChecksum(value?: string) {
  return value ? `${value.slice(0, 16)}...` : '-'
}
</script>

<template>
  <section class="page-card stack">
    <div class="page-header">
      <div>
        <h1>部署包管理</h1>
        <p>按客户环境生成不可变部署包，包含 manifest.json、checksum.sha256、资源和配置。</p>
      </div>
      <div class="toolbar">
        <button class="button secondary" type="button" @click="loadPackages">刷新</button>
        <button class="button primary" type="button" @click="showBuildForm">生成部署包</button>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="actionMessage" class="success-message">{{ actionMessage }}</p>

    <form v-if="formVisible" class="panel form-grid" @submit.prevent="submitBuild">
      <label class="field">
        <span>客户 ID</span>
        <input v-model.number="packageForm.customerId" type="number" min="1" required />
      </label>
      <label class="field">
        <span>环境 ID</span>
        <input v-model.number="packageForm.environmentId" type="number" min="1" required />
      </label>
      <label class="field">
        <span>部署方案版本 ID</span>
        <input v-model.number="packageForm.deployPlanVersionId" type="number" min="1" required />
      </label>
      <label class="field">
        <span>部署包版本</span>
        <input v-model.trim="packageForm.packageVersion" maxlength="64" required placeholder="1.0.1" />
      </label>
      <label class="field field-wide">
        <span>备注</span>
        <textarea v-model.trim="packageForm.remark" maxlength="512" placeholder="本次交付说明" />
      </label>
      <div class="form-actions field-wide">
        <button class="button primary" type="submit">确认生成</button>
        <button class="button secondary" type="button" @click="formVisible = false">取消</button>
      </div>
    </form>

    <div v-if="loading" class="muted">部署包加载中...</div>
    <div v-else-if="packages.length === 0" class="empty-state">暂无部署包。</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>部署包编码</th>
          <th>版本</th>
          <th>客户/环境</th>
          <th>方案版本</th>
          <th>状态</th>
          <th>Checksum</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="packageBuild in packages" :key="packageBuild.id" :class="{ selected: selectedPackageId === packageBuild.id }">
          <td>{{ packageBuild.packageCode }}</td>
          <td>{{ packageBuild.packageVersion }}</td>
          <td>{{ packageBuild.customerId }} / {{ packageBuild.environmentId }}</td>
          <td>{{ packageBuild.deployPlanVersionId }}</td>
          <td><span class="badge" :class="packageBuild.buildStatus.toLowerCase()">{{ packageBuild.buildStatus }}</span></td>
          <td>{{ shortChecksum(packageBuild.checksum) }}</td>
          <td>{{ formatDate(packageBuild.createdAt) }}</td>
          <td class="actions">
            <button class="link-button" type="button" @click="selectPackage(packageBuild)">查看 manifest</button>
            <button class="link-button" type="button" @click="refreshStatus(packageBuild)">状态</button>
            <button class="link-button" type="button" @click="loadDownloadInfo(packageBuild)">下载信息</button>
            <button class="link-button" type="button" @click="loadExecutionPlan(packageBuild)">执行计划</button>
            <button class="link-button danger" type="button" @click="deletePackage(packageBuild)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selectedPackage" class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>{{ selectedPackage.packageCode }}</h2>
          <p class="muted">{{ selectedPackage.filePath }} · 不可变：{{ selectedPackage.immutable ? '是' : '否' }}</p>
        </div>
      </div>

      <p v-if="manifestError" class="error-message">{{ manifestError }}</p>
      <div v-if="downloadInfo" class="download-info">
        <strong>下载文件：</strong>{{ downloadInfo.filePath }}
        <br />
        <strong>checksum：</strong>{{ downloadInfo.checksum }}
      </div>

      <div v-if="executionPlan" class="exec-plan">
        <h3>离线部署执行计划（agent 脚本已同包）</h3>
        <table class="data-table">
          <thead>
            <tr><th>#</th><th>步骤编码</th><th>步骤名</th><th>类型</th><th>目标</th><th>说明</th></tr>
          </thead>
          <tbody>
            <tr v-for="step in executionPlan.steps" :key="step.order">
              <td>{{ step.order }}</td>
              <td>{{ step.stepCode }}</td>
              <td>{{ step.stepName }}</td>
              <td><span class="badge">{{ step.type }}</span></td>
              <td>{{ step.target || '-' }}</td>
              <td class="muted">{{ step.detail }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="manifestLoading" class="muted">manifest 加载中...</div>
      <template v-else-if="manifest">
        <p class="muted">checksum.sha256：{{ manifest.checksum }}</p>
        <pre class="code-block">{{ formattedManifest }}</pre>
      </template>
    </section>
  </section>
</template>
