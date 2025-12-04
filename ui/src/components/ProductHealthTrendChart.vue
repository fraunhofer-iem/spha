<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch, computed } from "vue";
import {
  Chart,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  LineController,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { store } from "../store";

// Register Chart.js components
Chart.register(CategoryScale, LinearScale, PointElement, LineElement, LineController, Title, Tooltip, Legend, Filler);

const props = defineProps<{
  productId: string;
}>();

const chartCanvas = ref<HTMLCanvasElement | null>(null);
let chartInstance: Chart<"line"> | null = null;

// Get product from store
const product = computed(() => store.getProductById(props.productId));

const renderChart = () => {
  if (!chartCanvas.value || !product.value) return;
  
  const { labels, data } = product.value.getHealthDataForChart();
  
  if (data.length === 0) return;
  
  // Destroy an existing chart instance if it exists
  if (chartInstance) {
    chartInstance.destroy();
  }
  
  chartInstance = new Chart(chartCanvas.value, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        label: 'Health Score',
        data,
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4,
        fill: true,
        pointRadius: 3,
        pointHoverRadius: 3,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: undefined,
        intersect: false,
      },
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          enabled: false
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          max: 100,
          grid: {
            display: false
          },
          ticks: {
            display: false
          }
        },
        x: {
          grid: {
            display: false
          },
          ticks: {
            display: false
          }
        }
      },
      elements: {
        point: {
          radius: 2
        }
      }
    }
  });
};

onMounted(renderChart);

watch(() => props.productId, renderChart);
watch(() => product.value?.results, renderChart, { deep: true });

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.destroy();
  }
});
</script>

<template>
  <div class="chart-container">
    <canvas ref="chartCanvas"></canvas>
  </div>
</template>

<style scoped>
.chart-container {
  position: relative;
  display: inline-block;
  width: 140px;
  height: 4rem;
}

.chart-container canvas {
  width: 100% !important;
  height: 100% !important;
}
</style>
