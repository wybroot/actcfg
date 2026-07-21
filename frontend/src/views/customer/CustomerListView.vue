<template>
  <section class="page-card">
    <div class="toolbar">
      <h2>客户管理</h2>
      <button class="btn-primary" @click="openCreate">+ 新增客户</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">加载中...</p>

    <table v-else class="data-table">
      <thead>
        <tr><th>编码</th><th>名称</th><th>简称</th><th>行业</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="c in customers" :key="c.id">
          <td>{{ c.customerCode }}</td>
          <td>{{ c.customerName }}</td>
          <td>{{ c.shortName ?? '-' }}</td>
          <td>{{ c.industry ?? '-' }}</td>
          <td><span :class="c.status === 'ENABLED' ? 'badge-ok' : 'badge-off'">{{ c.status }}</span></td>
          <td class="actions">
            <button @click="openEdit(c)">编辑</button>
            <button class="btn-danger" @click="confirmDelete(c)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 新增/编辑弹窗 -->
    <div v-if="dialog.show" class="modal-overlay" @click.self="dialog.show = false">
      <div class="modal">
        <h3>{{ dialog.mode === 'create' ? '新增客户' : '编辑客户' }}</h3>
        <div class="field">
          <label>客户编码</label>
          <input v-model="dialog.form.customerCode" :disabled="dialog.mode === 'edit'" placeholder="客户编码" />
        </div>
        <div class="field">
          <label>客户名称</label>
          <input v-model="dialog.form.customerName" placeholder="客户名称" />
        </div>
        <div class="field">
          <label>简称</label>
          <input v-model="dialog.form.shortName" placeholder="简称（选填）" />
        </div>
        <div class="field">
          <label>行业</label>
          <input v-model="dialog.form.industry" placeholder="行业（选填）" />
        </div>
        <p v-if="dialog.error" class="error">{{ dialog.error }}</p>
        <div class="modal-footer">
          <button @click="dialog.show = false">取消</button>
          <button class="btn-primary" @click="submitDialog" :disabled="dialog.loading">
            {{ dialog.loading ? '保存中...' : '保 存' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api, type Customer } from '../../api/http'

const customers = ref<Customer[]>([])
const loading = ref(false)
const error = ref('')

const dialog = ref({
  show: false, mode: 'create' as 'create' | 'edit',
  customerId: 0, loading: false, error: '',
  form: { customerCode: '', customerName: '', shortName: '', industry: '' }
})

async function load() {
  loading.value = true; error.value = ''
  try { customers.value = await api.customers() }
  catch (e: unknown) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
}
onMounted(load)

function openCreate() {
  dialog.value = { show: true, mode: 'create', customerId: 0, loading: false, error: '',
    form: { customerCode: '', customerName: '', shortName: '', industry: '' } }
}
function openEdit(c: Customer) {
  dialog.value = { show: true, mode: 'edit', customerId: c.id, loading: false, error: '',
    form: { customerCode: c.customerCode, customerName: c.customerName,
            shortName: c.shortName ?? '', industry: c.industry ?? '' } }
}

async function submitDialog() {
  dialog.value.loading = true; dialog.value.error = ''
  try {
    const f = dialog.value.form
    if (dialog.value.mode === 'create') {
      await api.createCustomer({ customerCode: f.customerCode, customerName: f.customerName,
        shortName: f.shortName || undefined, industry: f.industry || undefined })
    } else {
      await api.updateCustomer(dialog.value.customerId, { customerName: f.customerName,
        shortName: f.shortName || undefined, industry: f.industry || undefined })
    }
    dialog.value.show = false; await load()
  } catch (e: unknown) {
    dialog.value.error = e instanceof Error ? e.message : '保存失败'
  } finally { dialog.value.loading = false }
}

async function confirmDelete(c: Customer) {
  if (!confirm(`确认删除客户「${c.customerName}」？`)) return
  try { await api.deleteCustomer(c.id); await load() }
  catch (e: unknown) { alert(e instanceof Error ? e.message : '删除失败') }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h2 { margin: 0; font-size: 18px; }
.btn-primary { background: #4361ee; color: #fff; border: none; border-radius: 4px; padding: 6px 16px; cursor: pointer; }
.btn-danger { color: #e53e3e; background: none; border: 1px solid #e53e3e; border-radius: 4px; padding: 2px 8px; cursor: pointer; font-size: 12px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { border-bottom: 1px solid #eee; padding: 10px 12px; text-align: left; }
.data-table th { background: #f8f9fa; font-weight: 600; }
.actions { display: flex; gap: 8px; }
.actions button { font-size: 12px; padding: 2px 8px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; }
.actions button:hover { border-color: #4361ee; color: #4361ee; }
.badge-ok { color: #38a169; font-size: 12px; }
.badge-off { color: #999; font-size: 12px; }
.error { color: #e53e3e; font-size: 13px; margin-bottom: 8px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; border-radius: 8px; padding: 28px 32px; width: 400px; }
.modal h3 { margin: 0 0 20px; font-size: 16px; }
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.field input { width: 100%; box-sizing: border-box; padding: 7px 10px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.field input:focus { outline: none; border-color: #4361ee; }
.field input:disabled { background: #f5f5f5; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.modal-footer button { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; }
</style>

