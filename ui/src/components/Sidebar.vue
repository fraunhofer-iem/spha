<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import {Popover} from 'bootstrap';
import {store} from "../store.ts";


const route = useRoute();
const selectedProduct = computed(() => {
  if (route.name === 'product-details' && route.params.id && typeof route.params.id === 'string') {
    return store.getProductById(route.params.id)
  } else {
    return null
  }
});

const products = store.products

const emit = defineEmits<{
  sidebarToggle: [collapsed: boolean];
}>();

const isCollapsed = ref(false);

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
  emit('sidebarToggle', isCollapsed.value);
};

const handleImageClick = () => {
  if (isCollapsed.value) {
    isCollapsed.value = false;
    emit('sidebarToggle', isCollapsed.value);
  }
};

// Helper function to get popover attributes
const getPopoverAttrs = (content: string) => ({
  'data-bs-toggle': isCollapsed.value ? 'popover' : '',
  'data-bs-placement': isCollapsed.value ? 'right' : '',
  'data-bs-trigger': isCollapsed.value ? 'hover' : '',
  'data-bs-content': isCollapsed.value ? content : ''
});

// Helper function to get navigation link classes
const getNavLinkClasses = (routeName: string) => ({
  'active': route.name === routeName,
  'justify-content-center': isCollapsed.value,
  'px-3': !isCollapsed.value
});

let popovers: Popover[] = [];

const initializePopovers = () => {
  // Dispose of existing popovers
  popovers.forEach(popover => popover.dispose());
  popovers = [];

  // Initialize popovers for all elements with data-bs-toggle="popover"
  const popoverElements = document.querySelectorAll('[data-bs-toggle="popover"]');
  popoverElements.forEach(element => {
    const popover = new Popover(element as Element);
    popovers.push(popover);
  });
};

onMounted(() => {
  nextTick(() => {
    initializePopovers();
  });
  // Emit initial sidebar state
  emit('sidebarToggle', isCollapsed.value);
});

watch(isCollapsed, () => {
  nextTick(() => {
    initializePopovers();
  });
});
</script>

<template>
  <div class="sidebar border-end vh-100 position-fixed"
       :style="`width: ${isCollapsed ? '80px' : '250px'}; z-index: 1000;`">

    <!-- Logo Section -->
    <div class="mt-2 p-3 d-flex align-items-center justify-content-between">
      <img
          src="./../assets/img/SPHA_Icon_Light.svg"
          alt="Software Product Health Assistant"
          width="50"
          class="sidebar-logo"
          @click="handleImageClick"
          :style="{ cursor: isCollapsed ? 'e-resize' : 'default' }"
          v-bind="getPopoverAttrs('Open Sidebar')">

      <a class="btn ms-2"
         v-if="!isCollapsed"
         style="cursor:w-resize"
         @click="toggleSidebar"
         :data-bs-toggle="'popover'"
         :data-bs-placement="'bottom'"
         :data-bs-trigger="'hover'"
         :data-bs-content="'Collapse Sidebar'">
        <i class="bi bi-layout-sidebar" style="font-size: 1.5rem"></i>
      </a>
    </div>

    <!-- Navigation -->
    <div class="navigation px-3">
      <nav class="nav flex-column">

        <!-- Project Overview -->
        <router-link
            :to="{ name: 'projects-overview' }"
            class="nav-link nav-item d-flex align-items-center py-2 px-3 mb-2 rounded"
            :class="getNavLinkClasses('projects-overview')"
            v-bind="getPopoverAttrs('Project Overview')"
        >
          <i class="bi bi-grid-3x3-gap me-2" v-if="!isCollapsed"></i>
          <i class="bi bi-grid-3x3-gap" v-else></i>
          <span v-if="!isCollapsed">Overview</span>
        </router-link>

        <!-- Products -->
        <router-link
            :to="{ name: 'product-list' }"
            class="nav-link nav-item d-flex align-items-center py-2 px-3 mb-2 rounded"
            :class="getNavLinkClasses('product-list')"
            v-bind="getPopoverAttrs('Product List')"
        >
          <i class="bi bi-box me-2" v-if="!isCollapsed"></i>
          <i class="bi bi-box" v-else></i>
          <span v-if="!isCollapsed">Products</span>
        </router-link>

        <!-- Result Upload (collapsed state) -->
        <router-link
            v-if="isCollapsed"
            :to="{ name: 'result-upload' }"
            class="nav-link nav-item d-flex align-items-center py-2 px-3 mb-2 rounded"
            :class="getNavLinkClasses('result-upload')"
            v-bind="getPopoverAttrs('Result Upload')"
        >
          <i class="bi bi-cloud-upload"></i>
        </router-link>

        <!-- Products List with Scrollable Area -->
        <div v-if="!isCollapsed" class="products-section flex-grow-1 d-flex flex-column">
          <div class="products-scrollable">
            <router-link
                v-for="product in products"
                :key="product.id"
                :to="{ name: 'product-details', params: { id: product.id } }"
                class="nav-link sub-nav-item d-flex align-items-center py-2 px-3 mb-1 rounded ms-3"
                :class="{ 'active': selectedProduct?.id === product.id }"
            >
              <span :title="product.description">{{ product.name }}</span>
            </router-link>
          </div>

          <!-- Sticky Result Upload Button -->
          <div class="sticky-upload-btn">
            <router-link
                :to="{ name: 'result-upload' }"
                class="nav-link nav-item d-flex align-items-center py-2 px-3 mb-2 rounded"
                :class="getNavLinkClasses('result-upload')"
            >
              <i class="bi bi-cloud-upload me-2"></i>
              <span>Result Upload</span>
            </router-link>
          </div>
        </div>
      </nav>
    </div>

  </div>
</template>

<style scoped>
.sidebar {
  top: 0;
  left: 0;
  background: #f5f5fb;
  transition: width 0.3s ease;
  overflow-x: hidden;
  overflow-y: auto;
}


.navigation {
  padding-top: 1rem;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.products-section {
  min-height: 0;
  flex: 1;
}

.products-scrollable {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 300px);
}

.products-scrollable::-webkit-scrollbar {
  width: 4px;
}

.products-scrollable::-webkit-scrollbar-track {
  background: transparent;
}

.products-scrollable::-webkit-scrollbar-thumb {
  background: #c0c0d0;
  border-radius: 2px;
}

.products-scrollable::-webkit-scrollbar-thumb:hover {
  background: #a0a0b0;
}

.sticky-upload-btn {
  margin-top: auto;
  padding-top: 1rem;
  border-top: 1px solid #e0e0e9;
  background: #f5f5fb;
}

.nav-link {
  color: #4a4a5e;
  text-decoration: none;
  transition: all 0.2s ease;
  font-size: 0.95rem;
  border: none;
  background: transparent;
  position: relative;
}

.nav-item:hover {
  background: #e6e7ff;
  color: #6366f1;
}

.nav-item.active {
  background: #e6e7ff;
  color: #6366f1;
  font-weight: 500;
}

.sub-nav-item {
  font-size: 0.9rem;
  color: #6a6a7e;
  background: transparent;
}

.sub-nav-item:hover {
  background: #f0f1ff;
  color: #6366f1;
}

.sub-nav-item.active {
  background: #e6e7ff;
  color: #6366f1;
  font-weight: 500;
}

.nav-link span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.collapse-toggle {
  bottom: 2rem;
  right: -12px;
}

.collapse-toggle button,
.expand-toggle button {
  background: white;
  border: 1px solid #e0e0e9;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.collapse-toggle button:hover,
.expand-toggle button:hover {
  background: #f0f1ff;
  border-color: #6366f1;
}

.collapse-toggle button i,
.expand-toggle button i {
  font-size: 0.75rem;
  color: #6366f1;
}

/* Scrollbar styling */
.sidebar::-webkit-scrollbar {
  width: 6px;
}

.sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #c0c0d0;
  border-radius: 3px;
}

.sidebar::-webkit-scrollbar-thumb:hover {
  background: #a0a0b0;
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s ease, width 0.3s ease;
  }

  .sidebar.show {
    transform: translateX(0);
  }
}
</style>