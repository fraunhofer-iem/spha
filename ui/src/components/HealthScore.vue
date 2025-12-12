<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref, watch} from "vue";
import {ArcElement, Chart, DoughnutController, Legend, Tooltip,} from "chart.js";
import annotationPlugin from "chartjs-plugin-annotation";
import DashboardCard from "./DashboardCard.vue";
import HealthScoreModal from "./HealthScoreModal.vue";
import {blue_chart} from "../assets/styles/Colors.ts";
import type {Kpi, Product} from "../model/Result.ts";

// https://www.chartjs.org/chartjs-plugin-annotation/3.1.0/guide/types/doughnutLabel.html
Chart.register(
    DoughnutController,
    ArcElement,
    Tooltip,
    Legend,
    annotationPlugin,
);

const props = defineProps<{
  product: Product;
}>();

// Derived data from product
const currentScore = computed(() => props.product.getCurrentHealthScore() ?? 0);
const trend = computed(() => props.product.getHealthScoreTrend());
const trendAbs = computed(() => (trend.value == null ? null : Math.abs(trend.value)));
const lastRootKpi = computed<Kpi | null>(() => {
  const results = props.product.results;
  if (!results || results.length === 0) return null;
  return results[results.length - 1]!.root as Kpi;
});

// Modal state management
const showModal = ref(false);

const openModal = () => {
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const chartCanvas2 = ref<HTMLCanvasElement | null>(null);
let chartInstance: Chart<"doughnut"> | null = null;

const renderChart = () => {
  if (!chartCanvas2.value) return;

  const labels = [""];
  const score = currentScore.value;
  const bounded = Math.max(0, Math.min(100, score));
  const data = [bounded, 100 - bounded];

  if (chartInstance) {
    chartInstance.destroy();
  }

  const trendColor = trend.value == null ? "grey" : trend.value > 0 ? "green" : "red";
  const trendIcon = trend.value == null ? "bi-dash" : trend.value > 0 ? "+" : "-";

  const annotation: any = {
    dLabel: {
      type: "doughnutLabel",
      content: () => [
        `${bounded}/100`,
        trend.value == null ? "-" : `${trendIcon}${trendAbs.value}`,
        "score",
      ],
      font: [{size: 40, weight: "bold"}, {size: 20, weight: "bold"}, {size: 25}],
      color: ["black", trendColor, "grey"],
    },
  };

  chartInstance = new Chart(chartCanvas2.value, {
    type: "doughnut",
    data: {
      labels,
      datasets: [
        {
          data,
          borderWidth: 0,
          backgroundColor(ctx) {
            if (ctx.dataIndex == 1) {
              return "rgba(230, 230, 230, 0.5)";
            }
            return blue_chart;
          },
        },
      ],
    },
    options: {
      layout: {
        padding: 10,
      },
      responsive: true,
      aspectRatio: 0,
      maintainAspectRatio: true,
      cutout: "80%",
      circumference: 240,
      rotation: -120,
      interaction: {
        intersect: false,
        mode: false as any,
      },
      hover: {
        mode: undefined,
      },
      plugins: {
        annotation: {
          annotations: annotation,
        },
        legend: {
          display: false,
        },
      },
    },
  });
};

onMounted(renderChart);

watch(currentScore, renderChart);

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.destroy();
  }
});
</script>

<template>
  <DashboardCard title="Project Health Score">
    <div class="chart-container">
      <canvas ref="chartCanvas2"></canvas>
    </div>
    <footer>
      <div class="d-grid ps-4 pe-4 pt-4 pb-3">
        <button
            type="button"
            class="text-primary-emphasis fw-bold bg-primary-subtle btn btn-lg"
            @click="openModal"
        >
          Details
        </button>
      </div>
    </footer>
  </DashboardCard>

  <HealthScoreModal
      :show="showModal"
      v-if="lastRootKpi"
      :root-kpi="lastRootKpi"
      @close="closeModal"
  />
</template>

<style scoped>
.chart-container {
  height: 220px;
}
</style>
