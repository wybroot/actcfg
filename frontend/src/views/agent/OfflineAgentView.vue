<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  api,
  type AgentExecutionLog,
  type AgentExecutionReport,
  type AgentRetryRecordView,
  type AgentTask,
  type AgentTaskStatus,
  type CreateAgentTaskPayload,
  type ImportAgentReportPayload,
  type ReportAgentStatusPayload
} from '../../api/http'

const tasks = ref<AgentTask[]>([])
const logs = ref<AgentExecutionLog[]>([])
const reports = ref<AgentExecutionReport[]>([])
const retryRecords = ref<AgentRetryRecordView[]>([])
const selectedTaskId = ref<number>()
const loading = ref(false)
const logLoading = ref(false)
const reportLoading = ref(false)
const retryRecordLoading = ref(false)
const error = ref('')
const logError = ref('')
const reportError = ref('')
const retryRecordError = ref('')
const actionMessage = ref('')
const createFormVisible = ref(false)
const reportFormVisible = ref(false)
const importReportFormVisible = ref(false)

const createForm = reactive<CreateAgentTaskPayload>({
  packageBuildId: 1,
  taskType: 'OFFLINE_DEPLOY'
})

const reportForm = reactive<ReportAgentStatusPayload>({
  taskStatus: 'RUNNING',
  resultSummary: '',
  stepCode: 'DEPLOY',
  stepName: '执行部署',
  logLevel: 'INFO',
  logContent: ''
})

const importReportForm = reactive<ImportAgentReportPayload>({
  taskId: 0,
  executionHost: '',
  failedStep: '',
  failureReason: '',
  healthCheckResult: '',
  reportContent: ''
})

const selectedTask = computed(() => tasks.value.find((item) => item.id === selectedTaskId.value))
const unfinishedStatuses: AgentTaskStatus[] = ['PENDING', 'RUNNING', 'RETRYING']
const finishedStatuses: AgentTaskStatus[] = ['SUCCESS', 'FAILED', 'CANCELED']

onMounted(() => {
  loadTasks()
  loadReports()
  loadRetryRecords()
})

async function loadTasks() {
  loading.value = true
  error.value = ''
  try {
    tasks.value = await api.offlineTasks()
    if (!selectedTaskId.value && tasks.value.length > 0) {
      await selectTask(tasks.value[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Agent 任务加载失败'
  } finally {
    loading.value = false
  }
}

async function loadReports() {
  reportLoading.value = true
  reportError.value = ''
  try {
    reports.value = await api.agentReports()
  } catch (err) {
    reportError.value = err instanceof Error ? err.message : '执行报告加载失败'
  } finally {
    reportLoading.value = false
  }
}

async function loadRetryRecords() {
  retryRecordLoading.value = true
  retryRecordError.value = ''
  try {
    retryRecords.value = await api.agentRetryRecords()
  } catch (err) {
    retryRecordError.value = err instanceof Error ? err.message : '失败续跑记录加载失败'
  } finally {
    retryRecordLoading.value = false
  }
}

async function selectTask(task: AgentTask) {
  selectedTaskId.value = task.id
  await loadLogs(task.id)
}

async function loadLogs(taskId: number) {
  logLoading.value = true
  logError.value = ''
  try {
    logs.value = await api.offlineTaskLogs(taskId)
  } catch (err) {
    logError.value = err instanceof Error ? err.message : '执行日志加载失败'
  } finally {
    logLoading.value = false
  }
}

function showCreateForm() {
  Object.assign(createForm, { packageBuildId: 1, taskType: 'OFFLINE_DEPLOY' })
  createFormVisible.value = true
}

function showReportForm(task: AgentTask) {
  selectedTaskId.value = task.id
  Object.assign(reportForm, {
    taskStatus: 'RUNNING',
    resultSummary: '',
    stepCode: 'DEPLOY',
    stepName: '执行部署',
    logLevel: 'INFO',
    logContent: ''
  })
  reportFormVisible.value = true
}

async function submitCreate() {
  error.value = ''
  actionMessage.value = ''
  try {
    const created = await api.createOfflineTask(createForm)
    createFormVisible.value = false
    await loadTasks()
    await selectTask(created)
    actionMessage.value = `${created.taskCode} 已创建`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '任务创建失败'
  }
}

async function submitReport() {
  if (!selectedTask.value) {
    return
  }
  error.value = ''
  actionMessage.value = ''
  try {
    const updated = await api.reportOfflineTaskStatus(selectedTask.value.id, reportForm)
    reportFormVisible.value = false
    await loadTasks()
    await selectTask(updated)
    actionMessage.value = `${updated.taskCode} 状态已更新为 ${updated.taskStatus}`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '状态上报失败'
  }
}

async function cancelTask(task: AgentTask) {
  error.value = ''
  actionMessage.value = ''
  try {
    const updated = await api.cancelOfflineTask(task.id)
    await loadTasks()
    await selectTask(updated)
    actionMessage.value = `${updated.taskCode} 已取消`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '取消失败'
  }
}

async function retryTask(task: AgentTask) {
  error.value = ''
  actionMessage.value = ''
  try {
    const updated = await api.retryOfflineTask(task.id)
    await loadTasks()
    await selectTask(updated)
    await loadRetryRecords()
    actionMessage.value = `${updated.taskCode} 已触发续跑`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '续跑失败'
  }
}

function showImportReportForm(task: AgentTask) {
  selectedTaskId.value = task.id
  Object.assign(importReportForm, {
    taskId: task.id,
    executionHost: '',
    failedStep: '',
    failureReason: '',
    healthCheckResult: '',
    reportContent: ''
  })
  importReportFormVisible.value = true
}

async function submitImportReport() {
  error.value = ''
  actionMessage.value = ''
  try {
    const report = await api.importAgentReport(importReportForm)
    importReportFormVisible.value = false
    await loadReports()
    actionMessage.value = `${report.reportCode} 已导入`
  } catch (err) {
    error.value = err instanceof Error ? err.message : '执行报告导入失败'
  }
}

function canOperate(task: AgentTask) {
  return unfinishedStatuses.includes(task.taskStatus)
}

function canRetry(task: AgentTask) {
  return task.taskStatus === 'FAILED'
}

function canImportReport(task: AgentTask) {
  return finishedStatuses.includes(task.taskStatus)
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
        <h1>Agent 离线部署</h1>
        <p>管理离线部署任务、状态上报、失败续跑记录和执行日志。</p>
      </div>
      <div class="toolbar">
        <button class="button secondary" type="button" @click="loadTasks">刷新</button>
        <button class="button primary" type="button" @click="showCreateForm">创建离线任务</button>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="actionMessage" class="success-message">{{ actionMessage }}</p>

    <form v-if="createFormVisible" class="panel form-grid" @submit.prevent="submitCreate">
      <label class="field">
        <span>部署包 ID</span>
        <input v-model.number="createForm.packageBuildId" type="number" min="1" required />
      </label>
      <label class="field">
        <span>任务类型</span>
        <input v-model.trim="createForm.taskType" maxlength="64" required placeholder="OFFLINE_DEPLOY" />
      </label>
      <div class="form-actions field-wide">
        <button class="button primary" type="submit">创建任务</button>
        <button class="button secondary" type="button" @click="createFormVisible = false">取消</button>
      </div>
    </form>

    <form v-if="reportFormVisible && selectedTask" class="panel form-grid" @submit.prevent="submitReport">
      <label class="field">
        <span>任务状态</span>
        <select v-model="reportForm.taskStatus" required>
          <option value="RUNNING">RUNNING</option>
          <option value="RETRYING">RETRYING</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="FAILED">FAILED</option>
        </select>
      </label>
      <label class="field">
        <span>步骤编码</span>
        <input v-model.trim="reportForm.stepCode" maxlength="64" required placeholder="DEPLOY" />
      </label>
      <label class="field">
        <span>步骤名称</span>
        <input v-model.trim="reportForm.stepName" maxlength="128" required placeholder="执行部署" />
      </label>
      <label class="field">
        <span>日志级别</span>
        <input v-model.trim="reportForm.logLevel" maxlength="16" placeholder="INFO" />
      </label>
      <label class="field field-wide">
        <span>结果摘要</span>
        <textarea v-model.trim="reportForm.resultSummary" maxlength="1024" placeholder="任务执行进度或结果" />
      </label>
      <label class="field field-wide">
        <span>日志内容</span>
        <textarea v-model.trim="reportForm.logContent" maxlength="2048" placeholder="Agent 上报日志" />
      </label>
      <div class="form-actions field-wide">
        <button class="button primary" type="submit">上报状态</button>
        <button class="button secondary" type="button" @click="reportFormVisible = false">取消</button>
      </div>
    </form>

    <div v-if="loading" class="muted">Agent 任务加载中...</div>
    <div v-else-if="tasks.length === 0" class="empty-state">暂无 Agent 离线任务。</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>任务编码</th>
          <th>部署包</th>
          <th>类型</th>
          <th>状态</th>
          <th>开始时间</th>
          <th>完成时间</th>
          <th>结果摘要</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id" :class="{ selected: selectedTaskId === task.id }">
          <td>{{ task.taskCode }}</td>
          <td>{{ task.packageBuildId }}</td>
          <td>{{ task.taskType }}</td>
          <td><span class="badge" :class="task.taskStatus.toLowerCase()">{{ task.taskStatus }}</span></td>
          <td>{{ formatDate(task.startedAt) }}</td>
          <td>{{ formatDate(task.finishedAt) }}</td>
          <td>{{ task.resultSummary || '-' }}</td>
          <td class="actions">
            <button class="link-button" type="button" @click="selectTask(task)">查看日志</button>
            <button v-if="canOperate(task)" class="link-button" type="button" @click="showReportForm(task)">上报状态</button>
            <button v-if="canOperate(task)" class="link-button danger" type="button" @click="cancelTask(task)">取消</button>
            <button v-if="canRetry(task)" class="link-button" type="button" @click="retryTask(task)">续跑</button>
            <button v-if="canImportReport(task)" class="link-button" type="button" @click="showImportReportForm(task)">导入报告</button>
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selectedTask" class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>{{ selectedTask.taskCode }} 执行日志</h2>
          <p class="muted">{{ selectedTask.taskStatus }} · 部署包 ID：{{ selectedTask.packageBuildId }}</p>
        </div>
      </div>

      <p v-if="logError" class="error-message">{{ logError }}</p>
      <div v-if="logLoading" class="muted">执行日志加载中...</div>
      <div v-else-if="logs.length === 0" class="empty-state">暂无执行日志。</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>步骤编码</th>
            <th>步骤名称</th>
            <th>状态</th>
            <th>级别</th>
            <th>重试次数</th>
            <th>内容</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td>{{ log.stepCode }}</td>
            <td>{{ log.stepName }}</td>
            <td><span class="badge" :class="log.stepStatus.toLowerCase()">{{ log.stepStatus }}</span></td>
            <td>{{ log.logLevel }}</td>
            <td>{{ log.retryCount }}</td>
            <td>{{ log.logContent || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <form v-if="importReportFormVisible" class="panel form-grid" @submit.prevent="submitImportReport">
      <label class="field">
        <span>任务 ID</span>
        <input v-model.number="importReportForm.taskId" type="number" min="1" required readonly />
      </label>
      <label class="field">
        <span>执行主机</span>
        <input v-model.trim="importReportForm.executionHost" maxlength="128" placeholder="10.0.0.1" />
      </label>
      <label class="field">
        <span>失败步骤</span>
        <input v-model.trim="importReportForm.failedStep" maxlength="64" placeholder="可选" />
      </label>
      <label class="field">
        <span>健康检查结果</span>
        <input v-model.trim="importReportForm.healthCheckResult" maxlength="512" placeholder="可选" />
      </label>
      <label class="field field-wide">
        <span>失败原因</span>
        <textarea v-model.trim="importReportForm.failureReason" maxlength="1024" placeholder="可选" />
      </label>
      <label class="field field-wide">
        <span>报告内容</span>
        <textarea v-model.trim="importReportForm.reportContent" maxlength="8192" placeholder="Agent 本地部署报告原文" />
      </label>
      <div class="form-actions field-wide">
        <button class="button primary" type="submit">导入报告</button>
        <button class="button secondary" type="button" @click="importReportFormVisible = false">取消</button>
      </div>
    </form>

    <section class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>执行报告</h2>
          <p class="muted">Agent 离线部署完成后导入的本地执行报告。</p>
        </div>
        <button class="button secondary" type="button" @click="loadReports">刷新</button>
      </div>

      <p v-if="reportError" class="error-message">{{ reportError }}</p>
      <div v-if="reportLoading" class="muted">执行报告加载中...</div>
      <div v-else-if="reports.length === 0" class="empty-state">暂无执行报告。</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>报告编号</th>
            <th>任务</th>
            <th>部署包</th>
            <th>执行主机</th>
            <th>执行状态</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>失败步骤</th>
            <th>健康检查</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="report in reports" :key="report.id">
            <td>{{ report.reportCode }}</td>
            <td>{{ report.taskId }}</td>
            <td>{{ report.packageBuildId }}</td>
            <td>{{ report.executionHost || '-' }}</td>
            <td><span class="badge" :class="report.executionStatus.toLowerCase()">{{ report.executionStatus }}</span></td>
            <td>{{ formatDate(report.startedAt) }}</td>
            <td>{{ formatDate(report.finishedAt) }}</td>
            <td>{{ report.failedStep || '-' }}</td>
            <td>{{ report.healthCheckResult || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel stack">
      <div class="page-header compact">
        <div>
          <h2>失败续跑记录</h2>
          <p class="muted">按任务汇总的失败续跑次数和最终状态。</p>
        </div>
        <button class="button secondary" type="button" @click="loadRetryRecords">刷新</button>
      </div>

      <p v-if="retryRecordError" class="error-message">{{ retryRecordError }}</p>
      <div v-if="retryRecordLoading" class="muted">失败续跑记录加载中...</div>
      <div v-else-if="retryRecords.length === 0" class="empty-state">暂无失败续跑记录。</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>任务编码</th>
            <th>部署包</th>
            <th>失败步骤</th>
            <th>失败原因</th>
            <th>续跑次数</th>
            <th>最近续跑时间</th>
            <th>最终状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in retryRecords" :key="record.taskId">
            <td>{{ record.taskCode || '-' }}</td>
            <td>{{ record.packageBuildId ?? '-' }}</td>
            <td>{{ record.failedStep || '-' }}</td>
            <td>{{ record.failureReason || '-' }}</td>
            <td>{{ record.retryCount }}</td>
            <td>{{ formatDate(record.lastRetryAt) }}</td>
            <td>
              <span v-if="record.finalStatus" class="badge" :class="record.finalStatus.toLowerCase()">{{ record.finalStatus }}</span>
              <span v-else>-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </section>
</template>
