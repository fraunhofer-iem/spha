<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import {
    BarController,
    BarElement,
    CategoryScale,
    Chart,
    type ChartData,
    type ChartOptions,
    LinearScale,
    Title,
    Tooltip,
} from "chart.js";
import DashboardCard from "./DashboardCard.vue";
import HealthScoreModal from "./HealthScoreModal.vue";
import { background_grey, blue_chart } from "../assets/styles/Colors.ts";
import type { Kpi } from "../model/Result.ts";
import {
    getKpisOverThreshold,
    generateKpiSummaryText,
    getKpiStatusColor,
} from "../util/KpiService.ts";

Chart.register(
    BarController,
    BarElement,
    CategoryScale,
    LinearScale,
    Tooltip,
    Title,
);

interface KpiView {
    name: string;
    score: number; // 0-100
    description: string; // Tooltip content
}

interface Props extends Kpi {}

const root = defineProps<Props>();
const kpis: KpiView[] = root.children.map((child) => {
    return {
        name: child.displayName,
        score: child.score,
        description: " ",
    };
});

const chartCanvas = ref<HTMLCanvasElement | null>(null);
let chartInstance: Chart | null = null;

// Modal state
const showModal = ref(false);

// KPI analysis computed properties
const kpiAnalysis = computed(() => getKpisOverThreshold(root, 60));
const summaryText = computed(() => generateKpiSummaryText(kpiAnalysis.value));
const statusColor = computed(() =>
    getKpiStatusColor(kpiAnalysis.value.percentage),
);

const createChart = () => {
    if (!chartCanvas.value) return;

    const data: ChartData<"bar"> = {
        labels: kpis.map((kpi) => kpi.name),
        datasets: [
            {
                label: "KPI Score",
                barThickness: 60,
                data: kpis.map((kpi) => kpi.score),
                backgroundColor: blue_chart,
                borderWidth: 0,
                stack: "stack1",
                borderRadius: 8,
            },
            {
                label: "Track",
                barThickness: 60,
                data: kpis.map(() => 100),
                backgroundColor: background_grey,
                borderWidth: 0,
                stack: "stack1",
                borderRadius: 8,
            },
        ],
    };

    const options: ChartOptions<"bar"> = {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
            intersect: false,
            mode: false as any,
        },
        hover: {
            mode: undefined,
        },
        scales: {
            y: {
                stacked: false,
                display: false,
                grid: {
                    display: false,
                },
                beginAtZero: true,
                max: 101,
                title: {
                    display: true,
                    text: "Score",
                },
            },
            x: {
                grid: {
                    display: false,
                },
                ticks: {
                    font: {
                        size: 16,
                    },
                },
            },
        },
        plugins: {
            tooltip: {
                callbacks: {
                    label: (context) => {
                        return kpis[context.dataIndex]?.description || "";
                    },
                },
            },
            legend: {
                display: false,
            },
        },
    };

    chartInstance = new Chart(chartCanvas.value, {
        type: "bar",
        data,
        options,
    });
};

onMounted(() => {
    createChart();
});

onUnmounted(() => {
    chartInstance?.destroy();
});

// Redraw if props change
watch(
    () => root,
    () => {
        chartInstance?.destroy();
        createChart();
    },
    { deep: true },
);

const handleButtonClick = () => {
    showModal.value = true;
};

const handleCloseModal = () => {
    showModal.value = false;
};
</script>

<template>
    <DashboardCard title="Top-Level KPI Overview">
        <div class="container">
            <div class="row">
                <div class="col col-md-10 h-100">
                    <div
                        class="chart-container"
                        style="position: relative; height: 300px"
                    >
                        <canvas ref="chartCanvas"></canvas>
                    </div>
                </div>
                <div class="col col-md-2">
                    <div
                        class="d-flex flex-column justify-content-between h-100"
                    >
                        <div>
                            <p class="text-start" :class="statusColor">
                                {{ summaryText }}
                            </p>
                        </div>
                        <div class="mt-auto">
                            <div class="d-grid mb-2">
                                <button
                                    type="button"
                                    @click="handleButtonClick"
                                    class="text-primary-emphasis fw-bold bg-primary-subtle btn btn-lg"
                                >
                                    Details
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Top Level KPIs Modal -->
        <HealthScoreModal 
            :show="showModal" 
            :root-kpi="root" 
            @close="handleCloseModal" 
        />
    </DashboardCard>
</template>

<style scoped>
.chart-container {
    width: 100%;
}
</style>
