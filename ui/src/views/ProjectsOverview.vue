<script setup lang="ts">
import DashboardCard from "../components/DashboardCard.vue";
import { store } from "../store.ts";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import type { Kpi } from "../model/Result.ts";
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
} from 'chart.js';

// Register Chart.js components
Chart.register(CategoryScale, LinearScale, PointElement, LineElement, LineController, Title, Tooltip, Legend);

const products = store.products;

/**
 * Recursively traverse KPI hierarchy and collect unique KPI types with their result types
 * (Same logic as in EmptyKpiCard.vue)
 */
const getUniqueKpiResults = (kpi: Kpi): Map<string, string> => {
  const resultMap = new Map<string, string>();

  // Add the current KPI's result type if it exists, using id (typeId) as key
  if (kpi.resultType && kpi.id) {
    resultMap.set(kpi.id, kpi.resultType);
  }

  // Recursively process children
  if (kpi.children && kpi.children.length > 0) {
    kpi.children.forEach((child) => {
      const childResults = getUniqueKpiResults(child);
      // Merge child results, but don't overwrite existing entries (first occurrence wins)
      childResults.forEach((resultType, id) => {
        if (!resultMap.has(id)) {
          resultMap.set(id, resultType);
        }
      });
    });
  }

  return resultMap;
};

// Statistics for the first card
const totalProducts = computed(() => products.length);

const avgHealthScore = computed(() => {
  if (products.length === 0) return 0;
  const score = 0;
  return Math.round(products.reduce((acc, product) => {
    return acc + (product.getCurrentHealthScore() ?? 0);
  }, score) / products.length);
});

const avgDataAvailability = computed(() => {
  if (products.length === 0) return 0;
  
  let totalUniqueKpis = 0;
  let totalEmptyKpis = 0;
  
  products.forEach(product => {
    if (product.results.length > 0) {
      // Get the latest result for this product
      const lastResult = product.results[product.results.length - 1];
      if (lastResult && lastResult.root) {
        const uniqueResults = getUniqueKpiResults(lastResult.root);
        totalUniqueKpis += uniqueResults.size;
        
        // Count empty KPIs
        const emptyCount = Array.from(uniqueResults.values()).filter(
          resultType => resultType === "de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty"
        ).length;
        totalEmptyKpis += emptyCount;
      }
    }
  });
  
  if (totalUniqueKpis === 0) return 0;
  
  // Calculate data availability as the inverse of empty percentage
  const dataAvailableKpis = totalUniqueKpis - totalEmptyKpis;
  return Math.round((dataAvailableKpis / totalUniqueKpis) * 100);
});

// Chart for the second card
const chartCanvas = ref<HTMLCanvasElement | null>(null);
let chartInstance: Chart<"line"> | null = null;

// Generate distinct colors for each product
const generateColor = (index: number): string => {
  const colors = [
    'rgb(75, 192, 192)',
    'rgb(255, 99, 132)',
    'rgb(54, 162, 235)',
    'rgb(255, 206, 86)',
    'rgb(153, 102, 255)',
    'rgb(255, 159, 64)',
    'rgb(199, 199, 199)',
    'rgb(83, 102, 255)',
    'rgb(255, 99, 255)',
    'rgb(99, 255, 132)',
  ];
  return colors[index % colors.length]!;
};

const renderChart = () => {
  if (!chartCanvas.value) return;
  
  // Destroy an existing chart instance if it exists
  if (chartInstance) {
    chartInstance.destroy();
  }
  
  if (products.length === 0) return;
  
  // Collect all unique dates across all products
  const allDatesSet = new Set<string>();
  products.forEach(product => {
    const { labels } = product.getHealthDataForChart();
    labels.forEach(label => allDatesSet.add(label));
  });
  
  const allDates = Array.from(allDatesSet).sort((a, b) => {
    const dateA = new Date(a.split('/').reverse().join('-'));
    const dateB = new Date(b.split('/').reverse().join('-'));
    return dateA.getTime() - dateB.getTime();
  });
  
  // Create datasets for each product
  const datasets = products.map((product, index) => {
    const { labels, data } = product.getHealthDataForChart();
    
    // Map data to all dates (fill with null for missing dates)
    const mappedData = allDates.map(date => {
      const idx = labels.indexOf(date);
      return idx >= 0 ? (data[idx] ?? null) : null;
    });
    
    const color = generateColor(index);
    const bgColor = color.replace('rgb', 'rgba').replace(')', ', 0.1)');
    
    return {
      label: product.name,
      data: mappedData,
      borderColor: color,
      backgroundColor: bgColor,
      tension: 0.4,
      fill: false,
      pointRadius: 4,
      pointHoverRadius: 6,
      spanGaps: true, // Connect lines even with null values
    };
  });
  
  chartInstance = new Chart(chartCanvas.value, {
    type: 'line',
    data: {
      labels: allDates,
      datasets: datasets,
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'nearest',
        intersect: false,
      },
      plugins: {
        legend: {
          display: true,
          position: 'top',
        },
        tooltip: {
          enabled: true,
          callbacks: {
            title: (context) => {
              return context[0]?.label || '';
            },
            label: (context) => {
              const productName = context.dataset.label || '';
              const score = context.parsed.y;
              return `${productName}: ${score}`;
            }
          }
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          max: 100,
          title: {
            display: true,
            text: 'Health Score'
          }
        },
        x: {
          title: {
            display: true,
            text: 'Date'
          }
        }
      }
    }
  });
};

onMounted(renderChart);

watch(() => products.length, renderChart, { deep: true });
watch(() => products.map(p => p.results.length), renderChart, { deep: true });

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.destroy();
  }
});
</script>

<template>
  <div>
    <!-- Show message when no products exist -->
    <div v-if="totalProducts === 0" class="text-center py-5">
      <div class="row justify-content-center">
        <div class="col-md-6">
          <div class="card border-0 shadow-sm">
            <div class="card-body py-5">
              <i class="bi bi-inbox display-1 text-muted mb-4"></i>
              <h4 class="text-muted mb-3">No Products Available</h4>
              <p class="text-muted mb-4">
                There are currently no products in the system. Upload analysis results to get started.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Show dashboard when products exist -->
    <div v-else>
      <div class="row">
        <!-- First Card: Statistics Overview -->
        <div class="col-md-4 mb-3">
          <DashboardCard title="Products Overview">
            <div class="d-flex flex-column gap-4 p-3">
              <div class="text-center">
                <div class="display-4 fw-bold text-primary">{{ totalProducts }}</div>
                <div class="text-muted">Total Products</div>
              </div>
              
              <div class="text-center">
                <div class="display-4 fw-bold text-info">{{ avgDataAvailability }}%</div>
                <div class="text-muted">Data Availability</div>
              </div>
              
              <div class="text-center">
                <div class="display-4 fw-bold" :class="{
                  'text-success': avgHealthScore >= 70,
                  'text-warning': avgHealthScore >= 50 && avgHealthScore < 70,
                  'text-danger': avgHealthScore < 50
                }">{{ avgHealthScore }}</div>
                <div class="text-muted">Average Health Score</div>
              </div>
            </div>
          </DashboardCard>
        </div>

        <!-- Second Card: Health Trends Chart -->
        <div class="col-md-8 mb-3">
          <DashboardCard title="Health Trends Across Products">
            <div class="chart-container p-3">
              <canvas ref="chartCanvas"></canvas>
            </div>
          </DashboardCard>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chart-container {
  position: relative;
  height: 400px;
}

.chart-container canvas {
  width: 100% !important;
  height: 100% !important;
}
</style>