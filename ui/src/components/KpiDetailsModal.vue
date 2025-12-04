<script setup lang="ts">
import Modal from './Modal.vue';
import KpiAccordion from './KpiAccordion.vue';
import type {Kpi} from '../model/Result.ts';
import {computed} from 'vue';
import {isCriticalKpi, isWarningKpi, useKpiFilters} from '../composables/kpiUtils.ts';

interface Props {
  root: Kpi;
  show?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
});

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const handleClose = () => {
  emit('close');
};

const rootComputed = computed(() => props.root);
const {sortedKpisBySeverity} = useKpiFilters(rootComputed);

const getCriticalCount = computed(() => {
  return sortedKpisBySeverity.value.filter(kpi => isCriticalKpi(kpi)).length;
});

const getWarningCount = computed(() => {
  return sortedKpisBySeverity.value.filter(kpi => isWarningKpi(kpi)).length;
});
</script>

<template>
  <Modal
      title="Critical & Low Value KPIs"
      :show="show"
      @close="handleClose"
  >
    <div class="kpi-list">
      <div v-if="sortedKpisBySeverity.length > 0">
        <!-- Summary -->
        <div class="mb-4">
          <div v-if="getCriticalCount > 0" class="alert alert-danger mb-2">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            <strong>{{ getCriticalCount }}</strong> KPI{{ getCriticalCount !== 1 ? 's' : '' }} with critical values
          </div>
          <div v-if="getWarningCount > 0" class="alert alert-warning mb-2">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            <strong>{{ getWarningCount }}</strong> KPI{{ getWarningCount !== 1 ? 's' : '' }} with low values
          </div>
        </div>

        <!-- KPI Accordion -->
        <KpiAccordion
            :kpis="sortedKpisBySeverity"
            accordion-id="kpiDetailsAccordion"
            :use-bootstrap-toggle="true"
            threshold-badge-class="bg-info"
        />
      </div>
      <div v-else class="text-muted text-center py-4">
        <p>No KPIs with critical or low values found</p>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.kpi-list {
  max-height: 700px;
  overflow-y: auto;
}
</style>