<script setup lang="ts">
import type {Kpi} from '../model/Result.ts';
import { ref } from 'vue';
import { getBadgeClass, sortChildren } from '../composables/kpiUtils.ts';

interface Props {
  kpi: Kpi;
}

const props = defineProps<Props>();

// Accordion state management for nested KPIs
const isExpanded = ref(false);

const toggleAccordion = () => {
  isExpanded.value = !isExpanded.value;
};

// Check if KPI has children
const hasChildren = () => {
  return props.kpi.children && props.kpi.children.length > 0;
};
</script>

<template>
  <!-- If KPI has children, render as accordion -->
  <div v-if="hasChildren()" class="accordion-item mb-3">
    <h2 class="accordion-header">
      <button
          class="accordion-button collapsed kpi-header"
          type="button"
          @click="toggleAccordion"
          :aria-expanded="isExpanded"
          :aria-controls="`collapse-${kpi.id}`"
      >
        <div class="d-flex justify-content-between align-items-center w-100 me-3">
          <div class="kpi-info">
            <h6 class="mb-1 fw-bold">{{ kpi.displayName }}</h6>
            <div v-if="kpi.description" class="mt-1">
              <small class="text-muted">{{ kpi.description }}</small>
            </div>
            <div v-if="kpi.tags && kpi.tags.length > 0" class="mt-1">
              <span
                  v-for="tag in kpi.tags"
                  :key="tag"
                  class="badge bg-secondary me-1"
                  style="font-size: 0.7em;"
              >
                {{ tag }}
              </span>
            </div>
          </div>
          <div class="kpi-score text-end">
            <span v-if="kpi.score === -1" class="badge bg-secondary fs-6 px-3 py-2">
              Insufficient Data
            </span>
            <span v-else :class="`badge ${getBadgeClass(kpi)} fs-6 px-3 py-2`">
              {{ Math.round(kpi.score) }}/100
            </span>
          </div>
        </div>
      </button>
    </h2>
    <div
        :id="`collapse-${kpi.id}`"
        class="accordion-collapse collapse"
        :class="{ 'show': isExpanded }"
    >
      <div class="accordion-body">
        <!-- KPI Metadata -->
        <div v-if="kpi.thresholds && kpi.thresholds.length > 0" class="mb-3">
          <small class="text-muted">Thresholds:</small>
          <div class="mt-1">
            <span
                v-for="threshold in kpi.thresholds"
                :key="threshold.name"
                class="badge bg-light text-dark me-1"
            >
              {{ threshold.name }}: {{ threshold.value }}
            </span>
          </div>
        </div>
        
        <!-- Children KPIs - Recursive rendering -->
        <div v-if="sortChildren(kpi.children).length > 0">
          <h6 class="mb-3 text-muted">Sub-KPIs</h6>
          <KpiItem
              v-for="childKpi in sortChildren(kpi.children)"
              :key="childKpi.id"
              :kpi="childKpi"
          />
        </div>
        <div v-else-if="kpi.children && kpi.children.length === 0" class="text-muted text-center py-2">
          <small>No sub-KPIs available</small>
        </div>
      </div>
    </div>
  </div>
  
  <!-- If KPI has no children, render as simple item -->
  <div v-else class="kpi-item mb-3 p-3 border rounded">
    <div class="d-flex justify-content-between align-items-center">
      <div class="kpi-info">
        <h6 class="mb-1 fw-bold">{{ kpi.displayName }}</h6>
        <div v-if="kpi.description" class="mt-1">
          <small class="text-muted">{{ kpi.description }}</small>
        </div>
      </div>
      <div class="kpi-score text-end">
        <span v-if="kpi.score === -1" class="badge bg-secondary fs-6 px-3 py-2">
          Insufficient Data
        </span>
        <span v-else :class="`badge ${getBadgeClass(kpi)} fs-6 px-3 py-2`">
          {{ Math.round(kpi.score) }}/100
        </span>
      </div>
    </div>
    <div v-if="kpi.thresholds && kpi.thresholds.length > 0" class="mt-2">
      <small class="text-muted">Thresholds:</small>
      <div class="mt-1">
        <span
            v-for="threshold in kpi.thresholds"
            :key="threshold.name"
            class="badge bg-light text-dark me-1"
        >
          {{ threshold.name }}: {{ threshold.value }}
        </span>
      </div>
    </div>
    <div v-if="kpi.tags && kpi.tags.length > 0" class="mt-2">
      <small class="text-muted">Tags:</small>
      <div class="mt-1">
        <span
            v-for="tag in kpi.tags"
            :key="tag"
            class="badge bg-secondary me-1"
        >
          {{ tag }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kpi-item {
  transition: all 0.2s ease;
}

.kpi-item:hover {
  background-color: #f8f9fa;
}

.kpi-score .badge {
  min-width: 80px;
}

/* Accordion customizations to match HealthScoreModal styling */
.accordion-item {
  border: 1px solid #dee2e6;
  border-radius: 0.375rem !important;
}

.accordion-button.kpi-header {
  background-color: white;
  border: none;
  padding: 1rem;
  transition: all 0.2s ease;
}

.accordion-button.kpi-header:hover {
  background-color: #f8f9fa;
}

.accordion-button.kpi-header:focus {
  box-shadow: none;
  border-color: transparent;
}

.accordion-button.kpi-header:not(.collapsed) {
  background-color: white;
  color: inherit;
}

.accordion-button.kpi-header::after {
  margin-left: auto;
  flex-shrink: 0;
}

.accordion-collapse {
  border-top: 1px solid #dee2e6;
}

.accordion-body {
  padding: 1rem;
  background-color: #fafafa;
}
</style>