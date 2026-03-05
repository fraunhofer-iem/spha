<script setup lang="ts">
import { computed } from "vue";
import { useRoute, RouterLink } from "vue-router";
import { useMetricsCatalogue } from "../lib/useMetricsCatalogue";
import { renderMarkdown } from "../lib/markdown";
import { getRepoBranch, getRepoUrl } from "../lib/config";
import { buildDependencyMaps } from "../lib/metricGraph";
import { buildMetricFeedbackUrl } from "../lib/metricFeedback";
import MetricDependencyGraph from "../components/MetricDependencyGraph.vue";

const route = useRoute();
const { metrics, loading, error, phaseLabelMap, metricById, phases } = useMetricsCatalogue();

const metric = computed(() => {
  const id = String(route.params.id ?? "");
  return metricById.value.get(id) ?? null;
});

const renderedMarkdown = computed(() => {
  if (!metric.value) return "";
  return renderMarkdown(metric.value.markdown);
});

const feedbackUrl = computed(() => {
  if (!metric.value) return null;
  return buildMetricFeedbackUrl({
    id: metric.value.id,
    title: metric.value.title,
    sourcePath: metric.value.source_path,
  });
});

const dependencyMaps = computed(() => buildDependencyMaps(metrics.value));

const parentMetrics = computed(() => {
  if (!metric.value) return [];
  const parents = dependencyMaps.value.parentsById.get(metric.value.id) ?? [];
  return parents
    .map((id) => metricById.value.get(id))
    .filter((parent): parent is NonNullable<typeof parent> => Boolean(parent));
});

const childMetrics = computed(() => {
  if (!metric.value) return [];
  const children = dependencyMaps.value.childrenById.get(metric.value.id) ?? [];
  return children
    .map((id) => metricById.value.get(id))
    .filter((child): child is NonNullable<typeof child> => Boolean(child));
});

const relatedMetrics = computed(() => {
  if (!metric.value) return [];
  const tagSet = new Set(metric.value.tags ?? []);
  const toolSet = new Set(metric.value.related_tools ?? []);

  return metrics.value
    .filter((candidate) => candidate.id !== metric.value?.id)
    .map((candidate) => {
      let score = 0;
      for (const tag of candidate.tags ?? []) {
        if (tagSet.has(tag)) score += 2;
      }
      for (const tool of candidate.related_tools ?? []) {
        if (toolSet.has(tool)) score += 1;
      }
      if (candidate.phase === metric.value?.phase) score += 1;
      return { candidate, score };
    })
    .filter(({ score }) => score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 6)
    .map(({ candidate }) => candidate);
});

const sourceUrl = computed(() => {
  const repoUrl = getRepoUrl();
  if (!metric.value || !repoUrl) return null;
  const repoBranch = getRepoBranch();
  const normalized = repoUrl.replace(/\/+$/, "");
  return `${normalized}/blob/${repoBranch}/${metric.value.source_path}`;
});
</script>

<template>
  <section class="metric-detail section section--light">
    <div class="container">
      <RouterLink class="back-link" to="/metrics">← Back to all metrics</RouterLink>

      <div v-if="loading" class="state">Loading metric…</div>
      <div v-else-if="error" class="state state--error">{{ error }}</div>
      <div v-else-if="!metric" class="state">Metric not found.</div>

      <div v-else class="metric-detail__content">
        <header class="metric-detail__header">
          <p class="metric-phase">{{ phaseLabelMap.get(metric.phase) ?? metric.phase }}</p>
          <div class="metric-detail__title-row">
            <h1>{{ metric.title }}</h1>
            <div class="metric-detail__actions panel-actions">
              <a
                v-if="feedbackUrl"
                class="primary"
                :href="feedbackUrl"
                target="_blank"
                rel="noopener noreferrer"
              >
                Comment
              </a>
              <button
                v-else
                class="ghost"
                type="button"
                disabled
                title="Comment requires repository configuration."
              >
                Comment
              </button>
            </div>
          </div>
          <p class="metric-id">{{ metric.id }}</p>
        </header>

        <div class="metric-detail__meta">
          <div>
            <span class="meta-label">Tags</span>
            <p v-if="metric.tags?.length" class="meta-links">
              <RouterLink
                v-for="tag in metric.tags"
                :key="tag"
                class="meta-link"
                :to="{ name: 'metrics', query: { tag } }"
              >
                {{ tag }}
              </RouterLink>
            </p>
            <p v-else>—</p>
          </div>
          <div>
            <span class="meta-label">Tools</span>
            <p v-if="metric.related_tools?.length" class="meta-links">
              <RouterLink
                v-for="tool in metric.related_tools"
                :key="tool"
                class="meta-link"
                :to="{ name: 'metrics', query: { tool } }"
              >
                {{ tool }}
              </RouterLink>
            </p>
            <p v-else>—</p>
          </div>
          <div>
            <span class="meta-label">Thresholds</span>
            <p>{{ metric.thresholds?.length ?? 0 }}</p>
          </div>
          <div>
            <span class="meta-label">Depends on</span>
            <p>{{ metric.depends_on?.length ?? 0 }}</p>
          </div>
        </div>

        <section class="metric-detail__body" v-html="renderedMarkdown"></section>

        <section class="metric-detail__extras">
          <div>
            <h3>Thresholds</h3>
            <ul>
              <li v-for="threshold in metric.thresholds ?? []" :key="threshold.name">
                <strong>{{ threshold.name }}:</strong> {{ threshold.value }}
                <span v-if="threshold.description"> — {{ threshold.description }}</span>
              </li>
              <li v-if="!metric.thresholds?.length">No thresholds defined.</li>
            </ul>
          </div>
          <div>
            <h3>Related metrics</h3>
            <ul>
              <li v-for="related in relatedMetrics" :key="related.id">
                <RouterLink :to="`/metrics/${related.id}`">{{ related.title }}</RouterLink>
              </li>
              <li v-if="relatedMetrics.length === 0">No related metrics yet.</li>
            </ul>
          </div>
          <div>
            <h3>References</h3>
            <ul>
              <li v-for="reference in metric.references ?? []" :key="reference">
                <a :href="reference" target="_blank" rel="noopener noreferrer">{{ reference }}</a>
              </li>
              <li v-if="!metric.references?.length">No references.</li>
            </ul>
          </div>
          <div>
            <h3>Source file</h3>
            <p v-if="sourceUrl">
              <a :href="sourceUrl" target="_blank" rel="noopener noreferrer">{{
                metric.source_path
              }}</a>
            </p>
            <p v-else>
              <code>{{ metric.source_path }}</code>
            </p>
          </div>
        </section>

        <MetricDependencyGraph
          :metric="metric"
          :parents="parentMetrics"
          :children="childMetrics"
          :phases="phases"
        />
      </div>
    </div>
  </section>
</template>
