<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">交付编排平台</div>
      <RouterLink v-for="route in routes" :key="route.path" :to="route.path" class="nav-link">
        {{ route.label }}
      </RouterLink>
    </aside>
    <div class="content-wrap">
      <header class="topbar">
        <span class="user-info">
          {{ currentUser?.displayName ?? currentUser?.username }}
          <span class="role-tag" v-if="currentUser?.roles?.length">
            {{ currentUser.roles[0] }}
          </span>
        </span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </header>
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useRouter } from 'vue-router'
import { routes } from '../router'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const auth = useAuth()
const { currentUser } = auth

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; }
.content-wrap { display: flex; flex-direction: column; flex: 1; overflow: hidden; }
.topbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 0 24px;
  height: 48px;
  background: #fff;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.user-info { font-size: 14px; color: #333; display: flex; align-items: center; gap: 8px; }
.role-tag {
  font-size: 11px;
  background: #e8f0fe;
  color: #4361ee;
  border-radius: 4px;
  padding: 1px 6px;
}
.logout-btn {
  font-size: 13px;
  padding: 4px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  color: #555;
}
.logout-btn:hover { border-color: #e53e3e; color: #e53e3e; }
.main { flex: 1; overflow-y: auto; padding: 24px; background: #f5f6fa; }
</style>

