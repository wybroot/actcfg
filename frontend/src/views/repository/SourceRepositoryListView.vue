<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  api,
  type CreateSourceRepositoryPayload,
  type SourceRepository,
  type SourceRepositoryType
} from '../../api/http'
import { useAuth } from '../../composables/useAuth'

const { hasAnyRole } = useAuth()
const canWrite = computed(() => hasAnyRole('SUPER_ADMIN', 'OPS'))

const repos = ref<SourceRepository[]>([])
const loading = ref(false)
const error = ref('')

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number>()
const saving = ref(false)
const form = reactive<CreateSourceRepositoryPayload>({
  repoCode: '', repoName: '', repoType: 'HARBOR', baseUrl: '',
  username: '', password: '', description: '', status: 'ENABLED'
})

const REPO_TYPES: SourceRepositoryType[] = ['HARBOR', 'NEXUS', 'MAVEN', 'GENERIC']

async function loadRepos() {
  loading.value = true; error.value = ''
  try {
    repos.value = await api.sourceRepositories()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '源仓库加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = undefined
  Object.assign(form, {
    repoCode: '', repoName: '', repoType: 'HARBOR', baseUrl: '',
    username: '', password: '', description: '', status: 'ENABLED'
  })
  dialogVisible.value = true
}

function openEdit(r: SourceRepository) {
  dialogMode.value = 'edit'
  editingId.value = r.id
  Object.assign(form, {
    repoCode: r.repoCode, repoName: r.repoName, repoType: r.repoType, baseUrl: r.baseUrl,
    username: r.username ?? '', password: '', description: r.description ?? '', status: r.status
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true; error.value = ''
  try {
    if (dialogMode.value === 'create') {
      await api.createSourceRepository({ ...form })
    } else if (editingId.value) {
      await api.updateSourceRepository(editingId.value, {
        repoName: form.repoName, repoType: form.repoType, baseUrl: form.baseUrl,
        username: form.username, password: form.password, description: form.description, status: form.status
      })
    }
    dialogVisible.value = false
    await loadRepos()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove(r: SourceRepository) {
  if (!window.confirm(`确认删除源仓库「${r.repoName}」？`)) return
  try {
    await api.deleteSourceRepository(r.id)
    await loadRepos()
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

const testing = ref<number>()
async function testConn(r: SourceRepository) {
  testing.value = r.id
  try {
    const result = await api.testSourceRepository(r.id)
    alert(result.success ? `✓ ${result.message}` : `✗ ${result.message}`)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '测试失败')
  } finally {
    testing.value = undefined
  }
}

onMounted(loadRepos)
</script>

<template>
  <section class="page-card stack">
    <div class="page-header">
      <div>
        <h1>源仓库</h1>
        <p class="muted">管理镜像/制品来源（Harbor 等）。平台只同步元数据，实际镜像仍从源仓库拉取。</p>
      </div>
      <button v-if="canWrite" class="button primary" @click="openCreate">+ 新增源仓库</button>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="loading" class="muted">加载中...</p>
    <div v-else-if="repos.length === 0" class="empty-state">暂无源仓库。</div>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>编码</th><th>名称</th><th>类型</th><th>地址</th><th>账号</th><th>状态</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in repos" :key="r.id">
          <td>{{ r.repoCode }}</td>
          <td>{{ r.repoName }}</td>
          <td>{{ r.repoType }}</td>
          <td class="mono">{{ r.baseUrl }}</td>
          <td>{{ r.username || '-' }}</td>
          <td><span class="badge" :class="r.status === 'ENABLED' ? 'success' : 'canceled'">{{ r.status }}</span></td>
          <td class="actions">
            <button class="link-button" @click="testConn(r)" :disabled="testing === r.id">
              {{ testing === r.id ? '测试中...' : '测试连接' }}
            </button>
            <template v-if="canWrite">
              <button class="link-button" @click="openEdit(r)">编辑</button>
              <button class="link-button danger" @click="remove(r)">删除</button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>

    <form v-if="dialogVisible" class="panel form-grid" @submit.prevent="save">
      <h3 class="field-wide">{{ dialogMode === 'create' ? '新增源仓库' : '编辑源仓库' }}</h3>
      <label class="field">
        <span>编码</span>
        <input v-model.trim="form.repoCode" required :readonly="dialogMode === 'edit'" placeholder="harbor-prod" />
      </label>
      <label class="field">
        <span>名称</span>
        <input v-model.trim="form.repoName" required placeholder="生产 Harbor" />
      </label>
      <label class="field">
        <span>类型</span>
        <select v-model="form.repoType">
          <option v-for="t in REPO_TYPES" :key="t" :value="t">{{ t }}</option>
        </select>
      </label>
      <label class="field">
        <span>状态</span>
        <select v-model="form.status">
          <option value="ENABLED">ENABLED</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </label>
      <label class="field field-wide">
        <span>地址</span>
        <input v-model.trim="form.baseUrl" required placeholder="https://harbor.example.com" />
      </label>
      <label class="field">
        <span>账号</span>
        <input v-model.trim="form.username" placeholder="admin" />
      </label>
      <label class="field">
        <span>密码{{ dialogMode === 'edit' ? '（留空不修改）' : '' }}</span>
        <input v-model="form.password" type="password" placeholder="••••••" />
      </label>
      <label class="field field-wide">
        <span>说明</span>
        <textarea v-model.trim="form.description" maxlength="512" placeholder="可选" />
      </label>
      <div class="form-actions field-wide">
        <button class="button secondary" type="button" @click="dialogVisible = false">取消</button>
        <button class="button primary" type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
      </div>
    </form>
  </section>
</template>
