const API_BASE = import.meta.env.VITE_API_BASE ?? ''

export async function get<T>(url: string): Promise<T> {
  const response = await fetch(`${API_BASE}${url}`)
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
  const body = await response.json()
  return body.data as T
}

export const api = {
  resources: () => get('/api/repository/resources'),
  deployPlans: () => get('/api/deploy/plans'),
  customers: () => get('/api/customers'),
  packages: () => get('/api/packages'),
  offlineTasks: () => get('/api/agents/offline/tasks'),
  operationLogs: () => get('/api/audit/operation-logs')
}
