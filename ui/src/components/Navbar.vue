<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from "vue";
import {store} from "../store.ts";
import {useRoute} from "vue-router";

const formattedTime = ref<string>('');

const route = useRoute()

const title = computed(() => {
  if (route.name === 'product-details' && route.params.id && typeof route.params.id === 'string') {
    const product = store.getProductById(route.params.id)
    return product?.name ?? ""
  }
  return ""
});


const updateTime = () => {
  const now = new Date();
  formattedTime.value = now.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  });
};

let intervalId: number;

onMounted(() => {
  updateTime();
  intervalId = window.setInterval(updateTime, 10000);
});

onUnmounted(() => {
  clearInterval(intervalId);
});
</script>

<template>
  <nav class="navbar">
    <div class="container mt-2 d-flex justify-content-between align-items-center">

      <h3 class="fw-bold mx-auto">{{ title }}</h3>
      <div class="d-flex align-items-center">
        <div class="me-3 time-display p-3">
          <p class="h5">{{ formattedTime }}</p>
        </div>
      </div>
    </div>
  </nav>
</template>

<style scoped>

</style>