<script setup lang="ts">
import DashboardCard from "./DashboardCard.vue";
import KpiDetailsModal from "./KpiDetailsModal.vue";
import {computed, ref} from "vue";
import type {Kpi} from "../model/Result.ts";
import { useKpiFilters } from "../composables/kpiUtils.ts";

const props = defineProps<{
  root: Kpi;
}>();

const rootComputed = computed(() => props.root);
const { criticalKpis, warningKpis } = useKpiFilters(rootComputed);

const warnings = computed(
    () => warningKpis.value.length != 0 || criticalKpis.value.length != 0,
);

// Modal state
const showModal = ref(false);

const handleButtonClick = () => {
  showModal.value = true;
};

const handleModalClose = () => {
  showModal.value = false;
};
</script>

<template>
  <DashboardCard title="Critical KPIs">
    <div v-if="warnings">
      <div class="align-items-start text-start">
        <div v-if="warningKpis.length > 0" class="alert alert-warning" role="alert">
          <div class="warning-header">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            <strong>Warning!</strong>
          </div>
          <div class="warning-content">
            {{ warningKpis.length }} KPIs have low values.
          </div>
        </div>
        <div v-if="criticalKpis.length > 0" class="alert alert-danger" role="alert">
          <div class="warning-header">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            <strong>Warning!</strong>
          </div>
          <div class="warning-content">
            {{ criticalKpis.length }} KPIs have critical values.
          </div>
        </div>
      </div>

      <footer>
        <div class="d-grid ps-4 pe-4 pt-4">
          <button
              type="button"
              @click="handleButtonClick"
              class="text-primary-emphasis fw-bold bg-primary-subtle btn btn-lg"
          >
            Details
          </button>
        </div>
      </footer>
    </div>
    <div v-else>
      <div class="align-items-start text-start">
        <div class="alert alert-success" role="alert">
          <div class="warning-header">
            <i class="bi bi-check-circle-fill me-2"></i>
            <strong>No critical KPIs detected</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- KPI Details Modal -->
    <KpiDetailsModal
        :root="root"
        :show="showModal"
        @close="handleModalClose"
    />
  </DashboardCard>
</template>

<style scoped>
.bi {
  font-size: 1.5rem; /* Make icons larger */
}

.warning-content {
  padding-left: 2.2rem; /* Align with the text after the icon */
}
</style>
