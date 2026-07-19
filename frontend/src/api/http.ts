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
  packages: () => get('/api/packages'),
  offlineTasks: () => get('/api/agents/offline/tasks'),
  operationLogs: () => get('/api/audit/operation-logs')
}
