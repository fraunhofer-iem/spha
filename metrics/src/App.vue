<script setup lang="ts">
import { RouterView } from "vue-router";
import AppFooter from "./components/AppFooter.vue";
import AppHeader from "./components/AppHeader.vue";
</script>

<template>
  <div class="app-shell">
    <AppHeader />
    <main class="app-content">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>
