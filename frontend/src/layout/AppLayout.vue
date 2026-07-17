<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">交付编排平台</div>
      <RouterLink v-for="route in routes" :key="route.path" :to="route.path" class="nav-link">
        {{ route.label }}
      </RouterLink>
    </aside>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { routes } from '../router'
</script>
