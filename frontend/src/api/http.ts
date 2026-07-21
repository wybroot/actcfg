export const API_BASE = import.meta.env.VITE_API_BASE ?? ''

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

// ---- Auth 相关类型 ----
export type LoginPayload = { username: string; password: string }
export type LoginResult  = { token: string; user: { id: number; username: string; displayName: string; roles: string[] } }
export type UserVO = { id: number; username: string; displayName: string; status: string; roles: string[]; createdAt: string }
export type RoleVO = { id: number; roleCode: string; roleName: string; status: string }
export type CreateUserPayload   = { username: string; displayName: string; password: string }
export type UpdateUserPayload   = { displayName: string }
export type AssignRolesPayload  = { roleIds: number[] }
export type ResetPasswordPayload = { newPassword: string }

export type ResourceType = 'JAR' | 'IMAGE' | 'SQL' | 'SCRIPT' | 'CONFIG' | 'PACKAGE'
export type ResourceSourceType = 'UPLOAD' | 'HARBOR' | 'NEXUS' | 'MAVEN' | 'INTERNAL_REPO'
export type ResourceStatus = 'ENABLED' | 'DISABLED'

export type Resource = {
  id: number
  resourceCode: string
  resourceName: string
  resourceType: ResourceType
  sourceType: ResourceSourceType
  description?: string
  status: ResourceStatus
  createdAt: string
  updatedAt: string
  deleted: boolean
}

export type ResourceVersion = {
  id: number
  resourceId: number
  version: string
  externalUrl?: string
  imageRepository?: string
  imageTag?: string
  checksum?: string
  releaseNote?: string
  status: ResourceStatus
  createdAt: string
}

export type CreateResourcePayload = {
  resourceCode: string
  resourceName: string
  resourceType: ResourceType
  sourceType: ResourceSourceType
  description?: string
  status?: ResourceStatus
}

export type UpdateResourcePayload = Omit<CreateResourcePayload, 'resourceCode'> & {
  status: ResourceStatus
}

export type CreateResourceVersionPayload = {
  version: string
  externalUrl?: string
  imageRepository?: string
  imageTag?: string
  checksum?: string
  releaseNote?: string
  status?: ResourceStatus
}

export type DeployPlanStatus = 'ENABLED' | 'DISABLED'
export type DeployPlanVersionStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED'

export type DeployPlan = {
  id: number
  planCode: string
  planName: string
  currentVersionId?: number
  status: DeployPlanStatus
  createdAt: string
}

export type DeployPlanVersion = {
  id: number
  planId: number
  version: string
  status: DeployPlanVersionStatus
  editable: boolean
  createdAt: string
}

export type DeployComponent = {
  id: number
  planVersionId: number
  componentName: string
  componentType: string
  resourceVersionId: number
  deployOrder: number
  configTemplate?: string
  healthCheck?: string
}

export type CustomerEnvironment = {
  id: number
  customerId: number
  environmentName: string
  environmentType: string
  deployPlanVersionId?: number
  status: string
}

export type Customer = {
  id: number
  customerCode: string
  customerName: string
  shortName?: string
  industry?: string
  status: string
}

export type EnvVariable = {
  id: number
  environmentId: number
  variableKey: string
  variableValue: string
  maskedValue?: string
  sensitive: boolean
}

export type CreateCustomerPayload = { customerCode: string; customerName: string; shortName?: string; industry?: string }
export type UpdateCustomerPayload = { customerName: string; shortName?: string; industry?: string }
export type CreateVariablePayload = { key: string; value: string; sensitive: boolean }
export type UpdateVariablePayload = { value: string; sensitive: boolean }
export type HarborSyncPayload     = { project: string; repository: string; tag: string; version?: string; releaseNote?: string }

export type Snapshot = {
  id: number
  customerId: number
  environmentId: number
  sourcePlanVersionId: number
  planName: string
  versionLabel: string
  status: string
  createdAt: string
}

export type SnapshotComponent = {
  id: number
  snapshotId: number
  componentName: string
  componentType: string
  resourceVersionId?: number
  deployOrder: number
  configTemplate?: string
  healthCheck?: string
}

export type SnapshotDetail = { snapshot: Snapshot; components: SnapshotComponent[] }

export type OperationLog = { id: number; operatorName: string; module: string; action: string; result: string; createdAt: string }
export type DownloadLog  = { id: number; downloaderName: string; targetType: string; targetName: string; ipAddress: string; createdAt: string }
export type LoginLog     = { id: number; username: string; loginResult: string; ipAddress: string; createdAt: string }

export type StatsOverview = {
  customerCount: number
  resourceCount: number
  packageCount: number
  agentTaskCount: number
  taskStatusCounts: Record<string, number>
}

export type Attribution = { label: string; count: number }

export type DeployStats = {
  totalTasks: number
  successCount: number
  failedCount: number
  canceledCount: number
  successRate: number
  topFailedSteps: Attribution[]
  topFailReasons: Attribution[]
}

export type PackageBuildStatus = 'BUILDING' | 'SUCCESS' | 'FAILED' | 'CANCELED'
export type PackageLifecycleState = 'ACTIVE' | 'ARCHIVED' | 'DEPRECATED' | 'PURGED'
export type AgentTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'RETRYING' | 'CANCELED'

export type PackageBuild = {
  id: number
  packageCode: string
  customerId: number
  environmentId: number
  deployPlanVersionId: number
  packageVersion: string
  buildStatus: PackageBuildStatus
  immutable: boolean
  filePath: string
  checksum: string
  createdAt: string
  lifecycleState: PackageLifecycleState
  downloadCount: number
  lastDownloadedAt: string | null
  retentionUntil: string | null
}

export type PackageManifest = {
  packageBuildId: number
  manifestJson: string
  checksum: string
}

export type PackageDownloadInfo = {
  packageBuildId: number
  packageCode: string
  filePath: string
  checksum: string
  manifestJson: string
}

export type DeployStepType = 'CHECK_ENV' | 'COMPAT_CHECK' | 'LOAD_IMAGE' | 'RENDER_CONFIG' | 'DB_INIT' | 'DEPLOY_ARTIFACT' | 'HEALTH_CHECK'
export type DeployStep = { order: number; stepCode: string; stepName: string; type: DeployStepType; target: string; detail: string }
export type ExecutionPlan = { packageCode: string; steps: DeployStep[] }

export type CreatePackageBuildPayload = {
  customerId: number
  environmentId: number
  deployPlanVersionId: number
  packageVersion: string
  remark?: string
}

export type AgentTask = {
  id: number
  taskCode: string
  packageBuildId: number
  taskType: string
  taskStatus: AgentTaskStatus
  startedAt?: string
  finishedAt?: string
  resultSummary?: string
}

export type AgentExecutionLog = {
  id: number
  taskId: number
  stepCode: string
  stepName: string
  stepStatus: AgentTaskStatus
  logLevel: string
  logContent?: string
  retryCount: number
}

export type CreateAgentTaskPayload = {
  packageBuildId: number
  taskType: string
}

export type ReportAgentStatusPayload = {
  taskStatus: AgentTaskStatus
  resultSummary?: string
  stepCode: string
  stepName: string
  logLevel?: string
  logContent?: string
}

export type AgentExecutionReport = {
  id: number
  reportCode: string
  taskId: number
  packageBuildId: number
  customerId: number
  environmentId: number
  executionHost?: string
  executionStatus: AgentTaskStatus
  startedAt?: string
  finishedAt?: string
  failedStep?: string
  failureReason?: string
  healthCheckResult?: string
  reportContent?: string
  importedAt: string
}

export type ImportAgentReportPayload = {
  taskId: number
  executionHost?: string
  failedStep?: string
  failureReason?: string
  healthCheckResult?: string
  reportContent?: string
}

export type AgentRetryRecordView = {
  taskId: number
  taskCode?: string
  packageBuildId?: number
  failedStep?: string
  failureReason?: string
  retryCount: number
  lastRetryAt?: string
  finalStatus?: AgentTaskStatus
}

export type BindDeployPlanPayload = {
  deployPlanVersionId: number
}

export type AgentInstance = {
  id: number
  agentCode: string
  hostname?: string
  ipAddress?: string
  customerId?: number
  environmentId?: number
  instanceStatus: string
  lastHeartbeatAt?: string
  registeredAt?: string
}

export type RegisterAgentPayload = {
  agentCode: string
  hostname?: string
  customerId?: number
  environmentId?: number
}

export type CreateDeployPlanPayload = {
  planCode: string
  planName: string
  description?: string
}

export type CreateDeployPlanVersionPayload = {
  version: string
}

export type CreateDeployComponentPayload = {
  componentName: string
  componentType: string
  resourceVersionId: number
  deployOrder: number
  configTemplate?: string
  healthCheck?: string
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem('delivery_token')
  const response = await fetch(`${API_BASE}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...init?.headers
    },
    ...init
  })
  // 401 → 清 token 并跳登录页
  if (response.status === 401) {
    localStorage.removeItem('delivery_token')
    localStorage.removeItem('delivery_user')
    window.location.href = '/login'
    throw new Error('未登录或 Token 失效')
  }
  const body = (await response.json()) as ApiResponse<T>
  if (!response.ok) {
    throw new Error(body?.message || `Request failed: ${response.status}`)
  }
  if (body.code !== 0) {
    throw new Error(body.message || '请求失败')
  }
  return body.data
}

export async function get<T>(url: string): Promise<T> {
  return request<T>(url)
}

export async function post<T>(url: string, data: unknown): Promise<T> {
  return request<T>(url, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export async function put<T>(url: string, data: unknown): Promise<T> {
  return request<T>(url, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

export async function del<T>(url: string): Promise<T> {
  return request<T>(url, {
    method: 'DELETE'
  })
}

export const api = {
  resources: () => get<Resource[]>('/api/repository/resources'),
  resource: (id: number) => get<Resource>(`/api/repository/resources/${id}`),
  createResource: (payload: CreateResourcePayload) => post<Resource>('/api/repository/resources', payload),
  updateResource: (id: number, payload: UpdateResourcePayload) => put<Resource>(`/api/repository/resources/${id}`, payload),
  deleteResource: (id: number) => del<void>(`/api/repository/resources/${id}`),
  resourceVersions: (id: number) => get<ResourceVersion[]>(`/api/repository/resources/${id}/versions`),
  createResourceVersion: (id: number, payload: CreateResourceVersionPayload) =>
    post<ResourceVersion>(`/api/repository/resources/${id}/versions`, payload),
  deployPlans: () => get<DeployPlan[]>('/api/deploy/plans'),
  deployPlan: (id: number) => get<DeployPlan>(`/api/deploy/plans/${id}`),
  createDeployPlan: (payload: CreateDeployPlanPayload) => post<DeployPlan>('/api/deploy/plans', payload),
  deployPlanVersions: (id: number) => get<DeployPlanVersion[]>(`/api/deploy/plans/${id}/versions`),
  createDeployPlanVersion: (id: number, payload: CreateDeployPlanVersionPayload) =>
    post<DeployPlanVersion>(`/api/deploy/plans/${id}/versions`, payload),
  publishDeployPlanVersion: (versionId: number) =>
    post<DeployPlanVersion>(`/api/deploy/plans/versions/${versionId}/publish`, {}),
  deployComponents: (versionId: number) =>
    get<DeployComponent[]>(`/api/deploy/plans/versions/${versionId}/components`),
  createDeployComponent: (versionId: number, payload: CreateDeployComponentPayload) =>
    post<DeployComponent>(`/api/deploy/plans/versions/${versionId}/components`, payload),
  packages: () => get<PackageBuild[]>('/api/packages'),
  packageBuild: (id: number) => get<PackageBuild>(`/api/packages/${id}`),
  createPackageBuild: (payload: CreatePackageBuildPayload) => post<PackageBuild>('/api/packages/build', payload),
  packageManifest: (id: number) => get<PackageManifest>(`/api/packages/${id}/manifest`),
  packageStatus: (id: number) => get<PackageBuildStatus>(`/api/packages/${id}/status`),
  packageDownloadInfo: (id: number) => get<PackageDownloadInfo>(`/api/packages/${id}/download`),
  packageExecutionPlan: (id: number) => get<ExecutionPlan>(`/api/packages/${id}/execution-plan`),
  deletePackageBuild: (id: number) => del<void>(`/api/packages/${id}`),
  archivePackage: (id: number) => put<PackageBuild>(`/api/packages/${id}/archive`, {}),
  deprecatePackage: (id: number) => put<PackageBuild>(`/api/packages/${id}/deprecate`, {}),
  cleanupPackages: () => post<number>('/api/packages/cleanup', {}),
  offlineTasks: () => get<AgentTask[]>('/api/agents/offline/tasks'),
  offlineTask: (id: number) => get<AgentTask>(`/api/agents/offline/tasks/${id}`),
  createOfflineTask: (payload: CreateAgentTaskPayload) => post<AgentTask>('/api/agents/offline/tasks', payload),
  cancelOfflineTask: (id: number) => post<AgentTask>(`/api/agents/offline/tasks/${id}/cancel`, {}),
  reportOfflineTaskStatus: (id: number, payload: ReportAgentStatusPayload) =>
    post<AgentTask>(`/api/agents/offline/tasks/${id}/status`, payload),
  offlineTaskLogs: (id: number) => get<AgentExecutionLog[]>(`/api/agents/offline/tasks/${id}/logs`),
  retryOfflineTask: (id: number) => post<AgentTask>(`/api/agents/offline/tasks/${id}/retry`, {}),
  agentRetryRecords: () => get<AgentRetryRecordView[]>('/api/agents/offline/retry-records'),
  importAgentReport: (payload: ImportAgentReportPayload) => post<AgentExecutionReport>('/api/agents/offline/reports/import', payload),
  agentReports: () => get<AgentExecutionReport[]>('/api/agents/offline/reports'),
  agentReport: (taskId: number) => get<AgentExecutionReport>(`/api/agents/offline/tasks/${taskId}/report`),

  // ---- 在线 Agent 实例 ----
  agentInstances: () => get<AgentInstance[]>('/api/agents/offline/instances'),
  registerAgent: (payload: RegisterAgentPayload) => post<AgentInstance>('/api/agents/offline/instances/register', payload),
  agentHeartbeat: (code: string) => post<AgentInstance>(`/api/agents/offline/instances/${code}/heartbeat`, {}),
  agentClaimTask: (code: string) => post<AgentTask | null>(`/api/agents/offline/instances/${code}/claim`, {}),

  // ---- 审计日志 ----
  operationLogs: () => get<OperationLog[]>('/api/audit/operation-logs'),
  downloadLogs:  () => get<DownloadLog[]>('/api/audit/download-logs'),
  loginLogs:     () => get<LoginLog[]>('/api/audit/login-logs'),

  // ---- 统计概览 ----
  statsOverview: () => get<StatsOverview>('/api/stats/overview'),
  deployStats: () => get<DeployStats>('/api/stats/deploy'),

  // ---- 配置快照 ----
  environmentSnapshot: (environmentId: number) => get<SnapshotDetail>(`/api/environments/${environmentId}/snapshot`),
  updateSnapshotComponentConfig: (snapshotId: number, componentId: number, configTemplate: string) =>
    put<SnapshotComponent>(`/api/snapshots/${snapshotId}/components/${componentId}/config`, { configTemplate }),

  // ---- 认证 ----
  login:          (payload: LoginPayload) => post<LoginResult>('/api/auth/login', payload),
  profile:        () => get<LoginResult['user']>('/api/auth/profile'),
  updateProfile:  (payload: { displayName: string }) => put<LoginResult['user']>('/api/auth/profile', payload),
  changePassword: (payload: { oldPassword: string; newPassword: string }) =>
    put<void>('/api/auth/password', payload),

  // ---- 用户管理 ----
  users:         () => get<UserVO[]>('/api/users'),
  user:          (id: number) => get<UserVO>(`/api/users/${id}`),
  createUser:    (payload: CreateUserPayload) => post<UserVO>('/api/users', payload),
  updateUser:    (id: number, payload: UpdateUserPayload) => put<UserVO>(`/api/users/${id}`, payload),
  deleteUser:    (id: number) => del<void>(`/api/users/${id}`),
  assignRoles:   (id: number, payload: AssignRolesPayload) => put<UserVO>(`/api/users/${id}/roles`, payload),
  resetPassword: (id: number, payload: ResetPasswordPayload) =>
    put<void>(`/api/users/${id}/password/reset`, payload),
  roles:         () => get<RoleVO[]>('/api/users/roles'),

  // ---- 客户管理 ----
  customers:        () => get<Customer[]>('/api/customers'),
  customer:         (id: number) => get<Customer>(`/api/customers/${id}`),
  createCustomer:   (payload: CreateCustomerPayload) => post<Customer>('/api/customers', payload),
  updateCustomer:   (id: number, payload: UpdateCustomerPayload) => put<Customer>(`/api/customers/${id}`, payload),
  deleteCustomer:   (id: number) => del<void>(`/api/customers/${id}`),

  // ---- 客户环境 ----
  customerEnvironments:     (customerId: number) => get<CustomerEnvironment[]>(`/api/customers/${customerId}/environments`),
  environment:              (id: number) => get<CustomerEnvironment>(`/api/environments/${id}`),
  bindEnvironmentDeployPlan:(environmentId: number, payload: BindDeployPlanPayload) =>
    put<CustomerEnvironment>(`/api/environments/${environmentId}/bind-plan`, payload),

  // ---- 环境变量 ----
  envVariables:   (environmentId: number) => get<EnvVariable[]>(`/api/environments/${environmentId}/variables`),
  createVariable: (environmentId: number, payload: CreateVariablePayload) =>
    post<EnvVariable>(`/api/environments/${environmentId}/variables`, payload),
  updateVariable: (environmentId: number, variableId: number, payload: UpdateVariablePayload) =>
    put<EnvVariable>(`/api/environments/${environmentId}/variables/${variableId}`, payload),
  deleteVariable: (environmentId: number, variableId: number) =>
    del<void>(`/api/environments/${environmentId}/variables/${variableId}`),
  cloneVariables: (toEnvironmentId: number, fromEnvironmentId: number) =>
    post<EnvVariable[]>(`/api/environments/${toEnvironmentId}/variables/clone-from/${fromEnvironmentId}`, {}),
  rotateSecrets: () => post<number>('/api/environments/variables/rotate-secrets', {}),

  // ---- 制品上传 & Harbor 同步 ----
  uploadResourceVersion: (resourceId: number, formData: FormData) =>
    request<ResourceVersion>(`/api/repository/resources/${resourceId}/versions/upload`, {
      method: 'POST',
      headers: {},   // 让浏览器自动设置 multipart boundary，不传 Content-Type
      body: formData,
    }),
  harborSync: (resourceId: number, payload: HarborSyncPayload) =>
    post<ResourceVersion>(`/api/repository/resources/${resourceId}/versions/harbor-sync`, payload),
}
