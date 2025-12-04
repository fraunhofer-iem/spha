<script setup lang="ts">
import type {Product} from '../model/Result.ts';
import {store} from "../store.ts";
import {useRouter} from "vue-router";
import {ref, computed} from "vue";
import ProductHealthTrendChart from '../components/ProductHealthTrendChart.vue';

const router = useRouter()

// Sorting state
const sortColumn = ref<string | null>(null)
const sortDirection = ref<'asc' | 'desc'>('desc')

// Computed sorted products
const products = computed(() => {
  const productList = [...store.products]
  
  if (sortColumn.value === 'healthScore') {
    return productList.sort((a, b) => {
      const scoreA = a.getCurrentHealthScore()
      const scoreB = b.getCurrentHealthScore()
      
      // Handle null values - put them at the end
      if (scoreA === null && scoreB === null) return 0
      if (scoreA === null) return 1
      if (scoreB === null) return -1
      
      const comparison = scoreA - scoreB
      return sortDirection.value === 'asc' ? comparison : -comparison
    })
  }
  
  return productList
})

// Sorting function
const sortBy = (column: string) => {
  if (sortColumn.value === column) {
    // Toggle direction if clicking the same column
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    // Set a new column and default to descending for health scores
    sortColumn.value = column
    sortDirection.value = 'desc'
  }
}

const onProductClick = (product: Product) => {
  router.push({name: 'product-details', params: {id: product.id}})
};
</script>

<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-12">

        <!-- Empty State -->
        <div v-if="products.length === 0" class="text-center py-5">
          <div class="card border-0 shadow-sm">
            <div class="card-body py-5">
              <i class="bi bi-inbox display-1 text-muted mb-3"></i>
              <h4 class="text-muted mb-3">No Products Available</h4>
              <p class="text-muted mb-4">Upload some result files to see your products here.</p>
              <div class="alert alert-info border-0" role="alert">
                <i class="bi bi-info-circle me-2"></i>
                Get started by uploading your first analysis result
              </div>
            </div>
          </div>
        </div>

        <!-- Products Table -->
        <div v-else class="card border-0 shadow-sm">


          <div class="table-responsive">
            <table class="table table-hover mb-0">
              <thead class="table-light">
              <tr>
                <th scope="col" class="fw-semibold text-dark py-3 ps-4">
                  <i class="bi bi-box me-2 text-primary"></i>
                  Product Name
                </th>
                <th scope="col" class="fw-semibold text-dark py-3">
                  <i class="bi bi-tag me-2 text-success"></i>
                  Version
                </th>
                <th scope="col" class="fw-semibold text-dark py-3">
                  <i class="bi bi-code-slash me-2 text-warning"></i>
                  Languages
                </th>
                <th scope="col" class="fw-semibold text-dark py-3 user-select-none sortable-header" 
                    @click="sortBy('healthScore')" 
                    style="cursor: pointer;">
                  <div class="d-flex align-items-center">
                    <i class="bi bi-heart-pulse me-2 text-danger"></i>
                    Health Score
                    <span class="ms-2">
                      <i v-if="sortColumn === 'healthScore' && sortDirection === 'desc'" class="bi bi-arrow-down text-primary"></i>
                      <i v-else-if="sortColumn === 'healthScore' && sortDirection === 'asc'" class="bi bi-arrow-up text-primary"></i>
                      <i v-else class="bi bi-arrow-down-up text-muted opacity-50"></i>
                    </span>
                  </div>
                </th>
                <th scope="col" class="fw-semibold text-dark py-3">
                  <i class="bi bi-graph-up me-2 text-success"></i>
                  Health Trend
                </th>
                <th scope="col" class="fw-semibold text-dark py-3 pe-4">
                  <i class="bi bi-link-45deg me-2 text-info"></i>
                  Repository
                </th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="product in products" :key="product.id" class="border-bottom clickable-row"
                  @click="onProductClick(product)">
                <td class="py-4 ps-4">
                  <div class="d-flex align-items-center">
                    <div class="bg-primary bg-opacity-10 rounded-circle p-2 me-3">
                      <i class="bi bi-box text-primary"></i>
                    </div>
                    <div>
                      <h6 class="fw-bold mb-1 text-dark">{{ product.name }}</h6>
                      <small class="text-muted">{{ product.description || 'No description available' }}</small>
                    </div>
                  </div>
                </td>
                <td class="py-4 align-middle">
                  <span class="badge bg-success bg-gradient px-3 py-2 fs-6 fw-normal">
                    <i class="bi bi-tag-fill me-1"></i>
                    {{ product.getNewestVersion() }}
                  </span>
                </td>
                <td class="py-4 align-middle">
                  <div class="language-tags">
                    <span class="badge bg-warning bg-gradient text-dark px-2 py-1 me-1 mb-1"
                          v-for="lang in product.getUsedLanguages().split(', ').slice(0, 3)"
                          :key="lang">
                      {{ lang }}
                    </span>
                    <span v-if="product.getUsedLanguages().split(', ').length > 3"
                          class="badge bg-light text-dark px-2 py-1">
                      +{{ product.getUsedLanguages().split(', ').length - 3 }} more
                    </span>
                  </div>
                </td>
                <td class="py-4 align-middle">
                  <div v-if="product.getCurrentHealthScore() !== null" class="d-flex align-items-center">
                    <span :class="`badge ${product.getHealthScoreColorClass()} px-3 py-2 fs-6 fw-normal me-2`">
                      <i class="bi bi-heart-pulse me-1"></i>
                      {{ product.getCurrentHealthScore() }}
                    </span>
                    <div v-if="product.getHealthScoreTrend() !== null" 
                         :class="`small ${product.getTrendColorClass()} fw-semibold`">
                      <i :class="`bi ${product.getTrendIcon()} me-1`"></i>
                      {{ Math.abs(product.getHealthScoreTrend()!) }}
                    </div>
                  </div>
                  <span v-else class="badge bg-light text-muted px-3 py-2">
                    <i class="bi bi-x-circle me-1"></i>
                    No Score
                  </span>
                </td>
                <td class="py-4 align-middle">
                  <ProductHealthTrendChart v-if="product.results.length > 1" :product-id="product.id" />
                  <div v-else class="text-muted small">
                    <i class="bi bi-info-circle me-1"></i>
                    Not enough data
                  </div>
                </td>
                <td class="py-4 pe-4 align-middle">
                  <a
                      v-if="product.getProjectUrl() !== '#'"
                      :href="product.getProjectUrl()"
                      target="_blank"
                      class="btn btn-outline-info btn-sm px-3 py-2 fw-semibold"
                  >
                    <i class="bi bi-box-arrow-up-right me-1"></i>
                    View Repository
                  </a>
                  <span v-else class="badge bg-light text-muted px-3 py-2">
                    <i class="bi bi-x-circle me-1"></i>
                    No URL
                  </span>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="card-footer bg-light border-0 py-3">
            <div class="row align-items-center">
              <div class="col">
                <small class="text-muted">
                  <i class="bi bi-info-circle me-1"></i>
                  Showing all {{ products.length }} products
                </small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.table th {
  border-top: none;
}

.badge {
  font-size: 0.75em;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
}

.clickable-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.clickable-row:hover {
  background-color: rgba(0, 123, 255, 0.05) !important;
}

.sortable-header:hover {
  background-color: rgba(0, 123, 255, 0.1) !important;
}

.sortable-header:active {
  background-color: rgba(0, 123, 255, 0.15) !important;
}
</style>