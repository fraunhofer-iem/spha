<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useMetricsCatalogue } from "../lib/useMetricsCatalogue";
import MetricCard from "../components/MetricCard.vue";
import { filterMetrics } from "../lib/metricsFilter";

const route = useRoute();
const { phases, metrics, loading, error, phaseLabelMap, metricSearchTextById } =
  useMetricsCatalogue();

const search = ref("");
const selectedPhases = ref<string[]>([]);
const selectedTags = ref<string[]>([]);
const selectedTools = ref<string[]>([]);
const requireThresholds = ref(false);
const requireDependencies = ref(false);
const openFilters = ref({
  phases: false,
  tags: false,
  tools: false,
  other: false,
});

function normalizeQueryArray(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map((entry) => String(entry)).filter(Boolean);
  }
  if (value === undefined || value === null || value === "") return [];
  return [String(value)];
}

function mergeQueryValues(...values: unknown[]): string[] {
  const set = new Set<string>();
  for (const value of values) {
    for (const entry of normalizeQueryArray(value)) {
      set.add(entry);
    }
  }
  return [...set];
}

watch(
  () => route.query,
  (query) => {
    const phaseSelections = normalizeQueryArray(query.phase);
    const tagSelections = mergeQueryValues(query.tag, query.tags);
    const toolSelections = mergeQueryValues(query.tool, query.tools);

    selectedPhases.value = phaseSelections;
    selectedTags.value = tagSelections;
    selectedTools.value = toolSelections;

    openFilters.value = {
      phases: phaseSelections.length > 0,
      tags: tagSelections.length > 0,
      tools: toolSelections.length > 0,
      other: false,
    };
  },
  { immediate: true },
);

const selectedPhaseSet = computed(() => new Set(selectedPhases.value));
const selectedTagSet = computed(() => new Set(selectedTags.value));
const selectedToolSet = computed(() => new Set(selectedTools.value));

const availableTags = computed(() => {
  const set = new Set<string>();
  for (const metric of metrics.value) {
    for (const tag of metric.tags ?? []) {
      set.add(tag);
    }
  }
  return [...set].sort((a, b) => a.localeCompare(b));
});

const availableTools = computed(() => {
  const set = new Set<string>();
  for (const metric of metrics.value) {
    for (const tool of metric.related_tools ?? []) {
      set.add(tool);
    }
  }
  return [...set].sort((a, b) => a.localeCompare(b));
});

const filteredMetrics = computed(() =>
  filterMetrics(metrics.value, {
    query: search.value,
    phaseFilter: selectedPhaseSet.value,
    tagFilter: selectedTagSet.value,
    toolFilter: selectedToolSet.value,
    requireThresholds: requireThresholds.value,
    requireDependencies: requireDependencies.value,
    searchTextById: metricSearchTextById.value,
  }),
);

const filteredCountLabel = computed(() => {
  if (loading.value || error.value) return "";
  return `${filteredMetrics.value.length} of ${metrics.value.length} metrics`;
});

function clearFilters() {
  search.value = "";
  selectedPhases.value = [];
  selectedTags.value = [];
  selectedTools.value = [];
  requireThresholds.value = false;
  requireDependencies.value = false;
}

function toggleFilter(key: keyof typeof openFilters.value) {
  openFilters.value[key] = !openFilters.value[key];
}
</script>

<template>
  <section class="metrics-section section section--light">
    <div class="container">
      <div class="section-header">
        <div>
          <p class="eyebrow">Catalogue</p>
          <h2>All metrics</h2>
        </div>
        <div class="section-subtitle">
          <p>Filter by phase, tags, tools, or dependencies. Matches any selected tag or tool.</p>
          <p class="count" v-if="filteredCountLabel">{{ filteredCountLabel }}</p>
        </div>
      </div>

      <div v-if="loading" class="state">Loading metrics catalogue…</div>
      <div v-else-if="error" class="state state--error">{{ error }}</div>
      <div v-else class="metrics-layout">
        <aside class="filters">
          <label class="search">
            <span>Search</span>
            <input
              v-model="search"
              type="search"
              placeholder="Search by title, id, or description"
            />
          </label>

          <div class="filter-group">
            <button
              class="filter-toggle"
              type="button"
              @click="toggleFilter('phases')"
              :aria-expanded="openFilters.phases"
              aria-controls="filter-phases"
            >
              <span>Phases</span>
              <span class="toggle-meta">
                <span class="toggle-text">{{ openFilters.phases ? "Hide" : "Show" }}</span>
                <span class="toggle-indicator" :class="openFilters.phases ? 'is-open' : ''">+</span>
              </span>
            </button>
            <div id="filter-phases" class="option-grid" v-show="openFilters.phases">
              <label v-for="phase in phases" :key="phase.id" class="option">
                <input type="checkbox" v-model="selectedPhases" :value="phase.id" />
                <span>{{ phase.name }}</span>
              </label>
            </div>
          </div>

          <div class="filter-group">
            <button
              class="filter-toggle"
              type="button"
              @click="toggleFilter('tags')"
              :aria-expanded="openFilters.tags"
              aria-controls="filter-tags"
            >
              <span>Tags</span>
              <span class="toggle-meta">
                <span class="toggle-text">{{ openFilters.tags ? "Hide" : "Show" }}</span>
                <span class="toggle-indicator" :class="openFilters.tags ? 'is-open' : ''">+</span>
              </span>
            </button>
            <div id="filter-tags" class="option-grid" v-show="openFilters.tags">
              <label v-for="tag in availableTags" :key="tag" class="option">
                <input type="checkbox" v-model="selectedTags" :value="tag" />
                <span>{{ tag }}</span>
              </label>
              <p v-if="availableTags.length === 0" class="empty">No tags yet.</p>
            </div>
          </div>

          <div class="filter-group">
            <button
              class="filter-toggle"
              type="button"
              @click="toggleFilter('tools')"
              :aria-expanded="openFilters.tools"
              aria-controls="filter-tools"
            >
              <span>Related tools</span>
              <span class="toggle-meta">
                <span class="toggle-text">{{ openFilters.tools ? "Hide" : "Show" }}</span>
                <span class="toggle-indicator" :class="openFilters.tools ? 'is-open' : ''">+</span>
              </span>
            </button>
            <div id="filter-tools" class="option-grid" v-show="openFilters.tools">
              <label v-for="tool in availableTools" :key="tool" class="option">
                <input type="checkbox" v-model="selectedTools" :value="tool" />
                <span>{{ tool }}</span>
              </label>
              <p v-if="availableTools.length === 0" class="empty">No tools yet.</p>
            </div>
          </div>

          <div class="filter-group">
            <button
              class="filter-toggle"
              type="button"
              @click="toggleFilter('other')"
              :aria-expanded="openFilters.other"
              aria-controls="filter-other"
            >
              <span>Other</span>
              <span class="toggle-meta">
                <span class="toggle-text">{{ openFilters.other ? "Hide" : "Show" }}</span>
                <span class="toggle-indicator" :class="openFilters.other ? 'is-open' : ''">+</span>
              </span>
            </button>
            <div id="filter-other" class="option-grid" v-show="openFilters.other">
              <label class="option">
                <input type="checkbox" v-model="requireThresholds" />
                <span>Has thresholds</span>
              </label>
              <label class="option">
                <input type="checkbox" v-model="requireDependencies" />
                <span>Has dependencies</span>
              </label>
            </div>
          </div>

          <button class="ghost" type="button" @click="clearFilters">Clear filters</button>
        </aside>

        <div class="metrics-grid">
          <MetricCard
            v-for="metric in filteredMetrics"
            :key="metric.id"
            :metric="metric"
            :phase-label="phaseLabelMap.get(metric.phase) ?? metric.phase"
          />

          <div v-if="filteredMetrics.length === 0" class="state">No metrics match the filters.</div>
        </div>
      </div>
    </div>
  </section>
</template>
