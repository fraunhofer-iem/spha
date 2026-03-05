<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { Metric } from "../lib/metrics";

defineProps<{
  metric: Metric;
  phaseLabel: string;
}>();
</script>

<template>
  <RouterLink :to="`/metrics/${metric.id}`" class="metric-card" data-testid="metric-card">
    <header>
      <p class="metric-phase">{{ phaseLabel }}</p>
      <h3>{{ metric.title }}</h3>
      <p class="metric-id">{{ metric.id }}</p>
    </header>

    <div class="metric-meta">
      <div>
        <span class="meta-label">Tags</span>
        <p>{{ metric.tags?.join(", ") || "—" }}</p>
      </div>
      <div>
        <span class="meta-label">Tools</span>
        <p>{{ metric.related_tools?.join(", ") || "—" }}</p>
      </div>
    </div>

    <div class="metric-footer">
      <span>Thresholds: {{ metric.thresholds?.length ?? 0 }}</span>
      <span>Depends on: {{ metric.depends_on?.length ?? 0 }}</span>
    </div>
  </RouterLink>
</template>
