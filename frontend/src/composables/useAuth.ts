import { reactive, readonly } from 'vue'

export type CurrentUser = {
  id: number
  username: string
  displayName: string
  roles: string[]
}

const TOKEN_KEY = 'delivery_token'
const USER_KEY  = 'delivery_user'

const state = reactive({
  token:       localStorage.getItem(TOKEN_KEY) ?? null as string | null,
  currentUser: (() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY) ?? 'null') as CurrentUser | null }
    catch { return null }
  })()
})

export function useAuth() {
  function login(token: string, user: CurrentUser) {
    state.token = token
    state.currentUser = user
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }

  function logout() {
    state.token = null
    state.currentUser = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function isLoggedIn(): boolean {
    return !!state.token
  }

  function hasRole(role: string): boolean {
    return state.currentUser?.roles.includes(role) ?? false
  }

  function hasAnyRole(...roles: string[]): boolean {
    return roles.some(r => hasRole(r))
  }

  return {
    token:       readonly(state).token,
    currentUser: readonly(state).currentUser,
    login,
    logout,
    isLoggedIn,
    hasRole,
    hasAnyRole
  }
}
