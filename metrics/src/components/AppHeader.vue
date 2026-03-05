<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { headerSocialLinks, proposeMetricUrl } from "../lib/config";
import logoUrl from "../assets/SPHA_Icon.svg";

const isScrolled = ref(false);
const isMetricsOpen = ref(false);
const isMenuOpen = ref(false);
const isMobile = ref(false);
const metricsMenuRef = ref<HTMLElement | null>(null);
const navRef = ref<HTMLElement | null>(null);
const route = useRoute();

const isMetricsActive = computed(() => {
  return route.path.startsWith("/metrics") || route.path.startsWith("/graph");
});

const isMetricsMenuOpen = computed(() => (isMobile.value ? true : isMetricsOpen.value));

function updateScrollState() {
  isScrolled.value = window.scrollY > 48;
}

function updateIsMobile() {
  isMobile.value = window.innerWidth <= 900;
}

function closeAllMenus() {
  isMetricsOpen.value = false;
  isMenuOpen.value = false;
}

function handleDocumentClick(event?: Event) {
  if (!event) {
    closeAllMenus();
    return;
  }
  const target = event.target as Node | null;
  if (target && navRef.value?.contains(target)) {
    if (metricsMenuRef.value && target && metricsMenuRef.value.contains(target)) return;
    if (!isMobile.value) {
      isMetricsOpen.value = false;
    }
    return;
  }
  closeAllMenus();
}

onMounted(() => {
  updateScrollState();
  updateIsMobile();
  window.addEventListener("scroll", updateScrollState, { passive: true });
  window.addEventListener("resize", updateIsMobile, { passive: true });
  document.addEventListener("click", handleDocumentClick);
});

onBeforeUnmount(() => {
  window.removeEventListener("scroll", updateScrollState);
  window.removeEventListener("resize", updateIsMobile);
  document.removeEventListener("click", handleDocumentClick);
});

watch(
  () => route.fullPath,
  () => {
    closeAllMenus();
  },
);
</script>

<template>
  <nav ref="navRef" class="navbar" :class="isScrolled ? 'navbar--floating' : 'navbar--banner'">
    <div class="navbar__inner">
      <RouterLink to="/" class="navbar__brand" @click="closeAllMenus">
        <img class="brand-logo" :src="logoUrl" alt="SPHA logo" />
        <span class="brand-text">
          <span class="brand-title">SPHA</span>
          <span class="brand-sub">Metric Catalogue</span>
        </span>
      </RouterLink>
      <button
        class="navbar__toggle"
        type="button"
        aria-controls="primary-navigation"
        :aria-expanded="isMenuOpen"
        @click="isMenuOpen = !isMenuOpen"
      >
        <span class="navbar__toggle-icon" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </span>
        <span class="navbar__toggle-text">Menu</span>
      </button>
      <div id="primary-navigation" class="navbar__links" :class="isMenuOpen ? 'is-open' : ''">
        <RouterLink
          to="/about"
          class="nav-link"
          active-class="nav-link--active"
          @click="closeAllMenus"
          >About
        </RouterLink>
        <div
          ref="metricsMenuRef"
          class="nav-dropdown"
          :class="isMetricsMenuOpen ? 'nav-dropdown--open' : ''"
        >
          <button
            v-if="!isMobile"
            class="nav-link nav-link--button"
            :class="isMetricsActive ? 'nav-link--active' : ''"
            type="button"
            aria-haspopup="true"
            :aria-expanded="isMetricsMenuOpen"
            aria-controls="metrics-menu"
            @click.stop="isMetricsOpen = !isMetricsOpen"
          >
            Metrics
            <span class="nav-dropdown__chevron" aria-hidden="true">▾</span>
          </button>
          <span v-else class="nav-link nav-link--static">Metrics</span>
          <div id="metrics-menu" class="nav-dropdown__menu" v-show="isMetricsMenuOpen">
            <RouterLink
              to="/metrics"
              class="nav-dropdown__link"
              active-class="nav-dropdown__link--active"
              @click="closeAllMenus"
            >
              List view
            </RouterLink>
            <RouterLink
              to="/graph"
              class="nav-dropdown__link"
              active-class="nav-dropdown__link--active"
              @click="closeAllMenus"
            >
              Graph view
            </RouterLink>
          </div>
        </div>
        <a
          class="nav-cta"
          :href="proposeMetricUrl"
          target="_blank"
          rel="noopener noreferrer"
          @click="closeAllMenus"
        >
          Propose a metric
        </a>
        <div v-if="!isScrolled" class="navbar__social">
          <a
            v-for="link in headerSocialLinks"
            :key="link.id"
            class="social-link"
            :href="link.url"
            target="_blank"
            rel="noopener noreferrer"
            :aria-label="link.ariaLabel"
            :title="link.title"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" :d="link.iconPath" />
            </svg>
          </a>
        </div>
      </div>
    </div>
  </nav>
</template>
