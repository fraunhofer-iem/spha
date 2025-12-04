<script setup lang="ts">
import ProjectOverview from "./../components/ProjectOverview.vue";
import RepoLanguagesPieChart from "./../components/RepoLanguagesPieChart.vue";
import HealthScore from "./../components/HealthScore.vue";
import TopLevelKpiOverview from "./../components/TopLevelKpiOverview.vue";
import KpiDetailsModal from "./../components/KpiDetailsModal.vue";
import ToolOverview from "./../components/ToolOverview.vue";
import EmptyKpiCard from "./../components/EmptyKpiCard.vue";

import {computed, ref} from "vue";
import {useKpiFilters} from "../composables/kpiUtils.ts";
import {store} from "../store.ts";

interface Props {
  id: string;
}

const props = defineProps<Props>();

// Use the selectedProduct prop directly
const product = computed(() =>
    store.getProductById(props.id)
);

// Check if product has any results
const hasResults = computed(() => (product.value?.results.length ?? 0) > 0);
const lastResult = computed(() => hasResults.value ? product.value!.results[product.value!.results.length - 1] : null);
const rootComputed = computed(() => lastResult.value?.root);
const {criticalKpis, warningKpis} = useKpiFilters(rootComputed);

const hasCritical = computed(() => hasResults.value && criticalKpis.value.length > 0);
const hasWarnings = computed(() => hasResults.value && warningKpis.value.length > 0);

// Modal state
const showModal = ref(false);

const handleAlertClick = () => {
  showModal.value = true;
};

const handleModalClose = () => {
  showModal.value = false;
};

const handleUploadClick = () => {
  // ToDo: open upload popup
};
</script>

<template>
  <!-- Show backup visualization when no results exist -->
  <div v-if="!hasResults" class="text-center py-5">
    <div class="row justify-content-center">
      <div class="col-md-6">
        <div class="card border-0 shadow-sm">
          <div class="card-body py-5">
            <i class="bi bi-inbox display-1 text-muted mb-4"></i>
            <h4 class="text-muted mb-3">No Results Available</h4>
            <p class="text-muted mb-4">
              There are currently no analysis results for the product "{{ product?.name }}".
              Upload a result file to view the dashboard.
            </p>
            <button
                type="button"
                class="btn btn-primary btn-lg"
                @click="handleUploadClick"
            >
              <i class="bi bi-upload me-2"></i>
              Upload Result
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Show normal dashboard when results exist -->
  <div v-else>
    <div v-if="hasCritical" class="alert alert-danger alert-dismissible" role="alert">
      <div>You have {{ criticalKpis.length }} critical KPIs. Click <a href="#" class="alert-link"
                                                                      @click.prevent="handleAlertClick">here</a> for
        details.
      </div>
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>
    <div v-if="hasWarnings" class="alert alert-warning alert-dismissible" role="alert">
      <div>You have {{ warningKpis.length }} warning KPIs. Click <a href="#" class="alert-link"
                                                                    @click.prevent="handleAlertClick">here</a> for
        details.
      </div>
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>
    <div class="row">
      <div class="col-md-3 mb-3">
        <HealthScore :product="product!"/>
      </div>

      <div class="col-md-9 mb-3">
        <TopLevelKpiOverview v-bind="lastResult!.root"/>
      </div>
    </div>

    <div class="row">
      <div class="col-md-6 mb-3">
        <ProjectOverview v-bind="lastResult!.repoInfo"/>
      </div>
      <div class="col-md-3 mb-3">
        <RepoLanguagesPieChart :languages="lastResult!.repoInfo.repoLanguages"/>
      </div>
      <div class="col-md-3 mb-3">
        <EmptyKpiCard :root="lastResult!.root"/>
      </div>
    </div>
    <div class="row">
      <div class="col-md-12 mb-3">
        <ToolOverview :tools="lastResult!.tools"/>
      </div>
    </div>

    <!-- KPI Details Modal -->
    <KpiDetailsModal
        :root="lastResult!.root"
        :show="showModal"
        @close="handleModalClose"
    />
  </div>
</template>
