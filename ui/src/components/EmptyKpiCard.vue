<script setup lang="ts">
import {computed, ref} from "vue";
import DashboardCard from "./DashboardCard.vue";
import Modal from "./Modal.vue";
import type {Kpi} from "../model/Result.ts";

const props = defineProps<{
  root: Kpi;
}>();

/**
 * Recursively traverse KPI hierarchy and collect unique KPI types with their result types
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
      // Merge child results but don't overwrite existing entries (first occurrence wins)
      childResults.forEach((resultType, id) => {
        if (!resultMap.has(id)) {
          resultMap.set(id, resultType);
        }
      });
    });
  }

  return resultMap;
};

/**
 * Calculate the percentage of empty KPI results based on unique typeIds
 */
const emptyKpiPercentage = computed(() => {
  const uniqueResults = getUniqueKpiResults(props.root);

  if (uniqueResults.size === 0) {
    return 0;
  }

  const emptyCount = Array.from(uniqueResults.values()).filter(
      resultType => resultType === "de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty"
  ).length;

  return Math.round((emptyCount / uniqueResults.size) * 100);
});

/**
 * Get the count of empty results for display (unique typeIds only)
 */
const emptyCount = computed(() => {
  const uniqueResults = getUniqueKpiResults(props.root);
  return Array.from(uniqueResults.values()).filter(
      resultType => resultType === "de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty"
  ).length;
});

/**
 * Get the total count of unique KPI types
 */
const totalCount = computed(() => {
  return getUniqueKpiResults(props.root).size;
});

/**
 * Get all empty KPIs with their details for modal display
 */
const getEmptyKpiDetails = (kpi: Kpi): Kpi[] => {
  const emptyKpis: Kpi[] = [];

  // Check if current KPI has an empty result
  if (kpi.resultType === "de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty") {
    emptyKpis.push(kpi);
  }

  // Recursively check children
  if (kpi.children && kpi.children.length > 0) {
    kpi.children.forEach((child) => {
      emptyKpis.push(...getEmptyKpiDetails(child));
    });
  }

  return emptyKpis;
};

/**
 * Get unique empty KPIs for modal display
 */
const emptyKpis = computed(() => {
  const allEmptyKpis = getEmptyKpiDetails(props.root);
  const uniqueEmptyKpis = new Map<string, Kpi>();

  allEmptyKpis.forEach(kpi => {
    if (!uniqueEmptyKpis.has(kpi.id)) {
      uniqueEmptyKpis.set(kpi.id, kpi);
    }
  });

  return Array.from(uniqueEmptyKpis.values());
});

// Modal state
const showModal = ref(false);

const openModal = () => {
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};
</script>

<template>
  <DashboardCard title="Missing KPI Results">
    <div class="content-wrapper">
      <div class="text-center">
        <div class="display-4 fw-bold text-warning mb-2">
          {{ emptyKpiPercentage }}%
        </div>
        <div class="text-muted mb-3">
          {{ emptyCount }} of {{ totalCount }} KPIs have empty results
        </div>
        <div class="progress" style="height: 10px;">
          <div
              class="progress-bar bg-warning"
              role="progressbar"
              :style="`width: ${emptyKpiPercentage}%`"
              :aria-valuenow="emptyKpiPercentage"
              aria-valuemin="0"
              aria-valuemax="100"
          ></div>
        </div>
      </div>
    </div>
    <footer>
      <div class="d-grid ps-4 pe-4 pt-4">
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

  <!-- Missing KPIs Modal -->
  <Modal
      title="Missing KPI Results"
      :show="showModal"
      @close="closeModal"
  >
    <div class="empty-kpi-list">
      <div v-if="emptyKpis.length > 0">
        <div class="alert alert-warning mb-4">
          <i class="bi bi-exclamation-triangle-fill me-2"></i>
          <strong>{{ emptyKpis.length }}</strong> KPI{{ emptyKpis.length !== 1 ? 's' : '' }} with missing results
        </div>

        <div class="list-group">
          <div
              v-for="kpi in emptyKpis"
              :key="kpi.id"
              class="list-group-item"
          >
            <div class="d-flex w-100 justify-content-between align-items-center">
              <div>
                <h6 class="mb-1">{{ kpi.displayName }}</h6>
                <small class="text-muted">ID: {{ kpi.id }}</small>
              </div>
              <span class="badge bg-secondary">Empty Result</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="text-muted text-center py-4">
        <p>No KPIs with missing results found</p>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.display-4 {
  font-size: 2.5rem;
}

.content-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 70%;
}
</style>