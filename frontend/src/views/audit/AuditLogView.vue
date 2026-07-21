<template>
  <section class="page-card">
    <div class="toolbar">
      <h2>日志审计</h2>
      <div class="tabs">
        <button :class="['tab', { active: tab === 'operation' }]" @click="tab = 'operation'">操作日志</button>
        <button :class="['tab', { active: tab === 'download' }]" @click="tab = 'download'">下载日志</button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">加载中...</p>

    <!-- 操作日志 -->
    <table v-else-if="tab === 'operation'" class="data-table">
      <thead>
        <tr><th>ID</th><th>操作人</th><th>模块</th><th>动作</th><th>结果</th><th>时间</th></tr>
      </thead>
      <tbody>
        <tr v-if="operationLogs.length === 0"><td colspan="6" class="empty">暂无操作日志</td></tr>
        <tr v-for="log in operationLogs" :key="log.id">
          <td>{{ log.id }}</td>
          <td>{{ log.operatorName }}</td>
          <td>{{ log.module }}</td>
          <td>{{ log.action }}</td>
          <td><span :class="log.result === 'SUCCESS' ? 'badge-ok' : 'badge-off'">{{ log.result }}</span></td>
          <td>{{ formatDate(log.createdAt) }}</td>
        </tr>
      </tbody>
    </table>

    <!-- 下载日志 -->
    <table v-else class="data-table">
      <thead>
        <tr><th>ID</th><th>下载人</th><th>目标类型</th><th>目标</th><th>IP</th><th>时间</th></tr>
      </thead>
      <tbody>
        <tr v-if="downloadLogs.length === 0"><td colspan="6" class="empty">暂无下载日志</td></tr>
        <tr v-for="log in downloadLogs" :key="log.id">
          <td>{{ log.id }}</td>
          <td>{{ log.downloaderName }}</td>
          <td>{{ log.targetType }}</td>
          <td>{{ log.targetName }}</td>
          <td>{{ log.ipAddress }}</td>
          <td>{{ formatDate(log.createdAt) }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { api, type OperationLog, type DownloadLog } from '../../api/http'

const tab = ref<'operation' | 'download'>('operation')
const operationLogs = ref<OperationLog[]>([])
const downloadLogs = ref<DownloadLog[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true; error.value = ''
  try {
    if (tab.value === 'operation') {
      operationLogs.value = await api.operationLogs()
    } else {
      downloadLogs.value = await api.downloadLogs()
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

watch(tab, load)
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h2 { margin: 0; font-size: 18px; }
.tabs { display: flex; gap: 4px; }
.tab { padding: 6px 14px; border: none; background: none; cursor: pointer; font-size: 14px; color: #666; border-bottom: 2px solid transparent; }
.tab.active { color: #4361ee; border-bottom-color: #4361ee; font-weight: 600; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { border-bottom: 1px solid #eee; padding: 10px 12px; text-align: left; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.empty { text-align: center; color: #999; padding: 24px; }
.badge-ok { color: #38a169; font-size: 12px; }
.badge-off { color: #e53e3e; font-size: 12px; }
.error { color: #e53e3e; font-size: 13px; margin-bottom: 8px; }
</style>
