import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import ResourceListView from '../views/repository/ResourceListView.vue'
import DeployPlanListView from '../views/deploy/DeployPlanListView.vue'
import CustomerListView from '../views/customer/CustomerListView.vue'
import EnvironmentListView from '../views/customer/EnvironmentListView.vue'
import PackageBuildListView from '../views/package/PackageBuildListView.vue'
import OfflineAgentView from '../views/agent/OfflineAgentView.vue'
import AuditLogView from '../views/audit/AuditLogView.vue'
import UserListView from '../views/system/UserListView.vue'

export const routes = [
  { path: '/', name: 'dashboard', label: '首页工作台', component: DashboardView },
  { path: '/repository/resources', name: 'resources', label: '产品仓库', component: ResourceListView },
  { path: '/deploy/plans', name: 'deployPlans', label: '部署配置', component: DeployPlanListView },
  { path: '/customers', name: 'customers', label: '客户管理', component: CustomerListView },
  { path: '/environments', name: 'environments', label: '客户环境', component: EnvironmentListView },
  { path: '/packages', name: 'packages', label: '部署包管理', component: PackageBuildListView },
  { path: '/agents/offline', name: 'offlineAgent', label: 'Agent 离线部署', component: OfflineAgentView },
  { path: '/audit/logs', name: 'auditLogs', label: '日志审计', component: AuditLogView },
  { path: '/system/users', name: 'users', label: '系统管理', component: UserListView }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})
