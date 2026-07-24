<template>
  <div class="login-page">
    <div class="login-card">
      <h1>交付编排平台</h1>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" required autofocus />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </div>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../api/http'
import { useAuth } from '../../composables/useAuth'

const router = useRouter()
const auth   = useAuth()

const form = ref({ username: '', password: '' })
const loading  = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  loading.value  = true
  try {
    const result = await api.login(form.value)
    auth.login(result.token, result.user)
    router.push('/')
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}
.login-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 16px rgba(0,0,0,.12);
  padding: 40px 48px;
  width: 360px;
}
h1 {
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 32px;
  color: #1a1a2e;
}
.field {
  margin-bottom: 20px;
}
label {
  display: block;
  font-size: 14px;
  color: #555;
  margin-bottom: 6px;
}
input {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color .2s;
}
input:focus { border-color: #4361ee; }
button {
  width: 100%;
  padding: 10px;
  background: #4361ee;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  cursor: pointer;
  transition: opacity .2s;
}
button:disabled { opacity: .6; cursor: not-allowed; }
button:hover:not(:disabled) { opacity: .9; }
.error { color: #e53e3e; font-size: 13px; margin-bottom: 12px; }
</style>
