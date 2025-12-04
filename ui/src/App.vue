<script setup lang="ts">
import {ref} from "vue";
import Navbar from "./components/Navbar.vue";
import Sidebar from "./components/Sidebar.vue";


const sidebarCollapsed = ref(false);
const onSidebarToggle = (collapsed: boolean) => {
  sidebarCollapsed.value = collapsed;
};

</script>

<template>
  <div class="d-flex">
    <!-- Sidebar -->
    <Sidebar
        @sidebar-toggle="onSidebarToggle"
    />

    <!-- Main Content Area -->
    <div class="flex-grow-1 main-content" :style="`margin-left: ${sidebarCollapsed ? '80px' : '250px'};`">
      <Navbar/>

      <div class="container-fluid mt-4">
        <router-view/>
      </div>
    </div>
  </div>
</template>

<style scoped>
.main-content {
  transition: margin-left 0.3s ease;
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0 !important;
  }
}
</style>
