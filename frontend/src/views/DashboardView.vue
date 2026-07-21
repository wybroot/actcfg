<template>
  <section class="page-card stack">
    <h1>首页工作台</h1>
    <p class="muted">企业私有化交付编排与自动部署平台 · 概览</p>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">加载中...</p>

    <template v-else-if="overview">
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-num">{{ overview.customerCount }}</div>
          <div class="stat-label">客户</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ overview.resourceCount }}</div>
          <div class="stat-label">制品</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ overview.packageCount }}</div>
          <div class="stat-label">部署包</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ overview.agentTaskCount }}</div>
          <div class="stat-label">Agent 任务</div>
        </div>
      </div>

      <div class="task-status">
        <h2>Agent 任务状态分布</h2>
        <div v-if="statusEntries.length === 0" class="muted">暂无任务</div>
        <ul v-else class="status-list">
          <li v-for="[status, count] in statusEntries" :key="status">
            <span class="badge" :class="status.toLowerCase()">{{ status }}</span>
            <span class="status-count">{{ count }}</span>
          </li>
        </ul>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type StatsOverview } from '../api/http'

const overview = ref<StatsOverview>()
const loading = ref(false)
const error = ref('')

const statusEntries = computed(() =>
  overview.value ? Object.entries(overview.value.taskStatusCounts) : []
)

onMounted(async () => {
  loading.value = true
  try {
    overview.value = await api.statsOverview()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '统计加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stack { display: flex; flex-direction: column; gap: 16px; }
.muted { color: #888; }
.error { color: #e53e3e; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { background: #f8f9fb; border: 1px solid #eee; border-radius: 8px; padding: 20px; text-align: center; }
.stat-num { font-size: 32px; font-weight: 700; color: #4361ee; }
.stat-label { margin-top: 6px; color: #666; font-size: 14px; }
.task-status { margin-top: 8px; }
.task-status h2 { font-size: 16px; margin-bottom: 12px; }
.status-list { list-style: none; padding: 0; display: flex; flex-wrap: wrap; gap: 16px; }
.status-list li { display: flex; align-items: center; gap: 8px; }
.badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; background: #edf2f7; color: #333; }
.badge.success { background: #c6f6d5; color: #22543d; }
.badge.failed { background: #fed7d7; color: #822727; }
.badge.running, .badge.retrying { background: #feebc8; color: #7b341e; }
.badge.canceled { background: #e2e8f0; color: #4a5568; }
.status-count { font-weight: 600; }
</style>
