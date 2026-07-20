const API_BASE = import.meta.env.VITE_API_BASE ?? ''

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

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

export type PackageBuildStatus = 'BUILDING' | 'SUCCESS' | 'FAILED' | 'CANCELED'
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
  const response = await fetch(`${API_BASE}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers
    },
    ...init
  })
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
  customers: () => get('/api/customers'),
  customerEnvironments: (customerId: number) => get<CustomerEnvironment[]>(`/api/customers/${customerId}/environments`),
  environment: (id: number) => get<CustomerEnvironment>(`/api/environments/${id}`),
  bindEnvironmentDeployPlan: (environmentId: number, payload: BindDeployPlanPayload) =>
    put<CustomerEnvironment>(`/api/environments/${environmentId}/bind-plan`, payload),
  packages: () => get<PackageBuild[]>('/api/packages'),
  packageBuild: (id: number) => get<PackageBuild>(`/api/packages/${id}`),
  createPackageBuild: (payload: CreatePackageBuildPayload) => post<PackageBuild>('/api/packages/build', payload),
  packageManifest: (id: number) => get<PackageManifest>(`/api/packages/${id}/manifest`),
  packageStatus: (id: number) => get<PackageBuildStatus>(`/api/packages/${id}/status`),
  packageDownloadInfo: (id: number) => get<PackageDownloadInfo>(`/api/packages/${id}/download`),
  deletePackageBuild: (id: number) => del<void>(`/api/packages/${id}`),
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
  operationLogs: () => get('/api/audit/operation-logs')
}
