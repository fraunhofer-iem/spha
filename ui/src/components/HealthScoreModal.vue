<script setup lang="ts">
import Modal from './Modal.vue';
import KpiAccordion from './KpiAccordion.vue';
import type {Kpi} from '../model/Result.ts';
import { computed, ref } from 'vue';
import { sortChildren } from '../composables/kpiUtils.ts';

interface Props {
  show: boolean;
  rootKpi: Kpi;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const handleClose = () => {
  emit('close');
};

// Sort KPIs: move items with score -1 to the end
const sortedKpis = computed(() => {
  if (!props.rootKpi.children || props.rootKpi.children.length === 0) {
    return [];
  }
  
  return sortChildren(props.rootKpi.children);
});

// Accordion state management
const expandedAccordions = ref<Set<string>>(new Set());

const toggleAccordion = (kpiId: string) => {
  if (expandedAccordions.value.has(kpiId)) {
    expandedAccordions.value.delete(kpiId);
  } else {
    expandedAccordions.value.add(kpiId);
  }
};

</script>

<template>
  <Modal
      :show="show"
      title="Health Score Details"
      @close="handleClose"
  >
    <div class="kpi-list">
      <h6 class="mb-3 text-muted">Top Level KPIs</h6>
      <div v-if="sortedKpis.length > 0">
        <KpiAccordion
            :kpis="sortedKpis"
            accordion-id="kpiAccordion"
            :use-bootstrap-toggle="false"
            threshold-badge-class="bg-light text-dark"
            :expanded-kpis="expandedAccordions"
            @toggle-accordion="toggleAccordion"
        />
      </div>
      <div v-else class="text-muted text-center py-4">
        <p>No KPI data available</p>
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