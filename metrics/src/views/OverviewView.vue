<script setup lang="ts">
import { computed } from "vue";
import { useMetricsCatalogue } from "../lib/useMetricsCatalogue";
import { proposeMetricUrl } from "../lib/config";
import type { Metric } from "../lib/metrics";
import iconUrl from "../assets/SPHA_Icon.svg";
import {
  CalendarCheck,
  Code2,
  Hammer,
  FlaskConical,
  Package,
  Rocket,
  Settings,
  Activity,
} from "lucide-vue-next";
import type { Component } from "vue";

const iconMap: Record<string, Component> = {
  "calendar-check": CalendarCheck,
  "code-2": Code2,
  hammer: Hammer,
  "flask-conical": FlaskConical,
  package: Package,
  rocket: Rocket,
  settings: Settings,
  activity: Activity,
};
const fallbackIcon = Activity;

const { phases, metrics, loading, error, formattedUpdatedAt, metricById } = useMetricsCatalogue();

const dependedOnInPhase = computed(() => {
  const map = new Map<string, Set<string>>();
  for (const phase of phases.value) {
    map.set(phase.id, new Set());
  }
  for (const metric of metrics.value) {
    const phase = metric.phase;
    const set = map.get(phase) ?? new Set<string>();
    for (const dependency of metric.depends_on ?? []) {
      if (metricById.value.get(dependency)?.phase === phase) {
        set.add(dependency);
      }
    }
    map.set(phase, set);
  }
  return map;
});

const topLevelByPhase = computed(() => {
  const map = new Map<string, Metric[]>();
  for (const phase of phases.value) {
    map.set(phase.id, []);
  }
  for (const metric of metrics.value) {
    const dependedOn = dependedOnInPhase.value.get(metric.phase);
    if (dependedOn?.has(metric.id)) {
      continue;
    }
    const bucket = map.get(metric.phase) ?? [];
    bucket.push(metric);
    map.set(metric.phase, bucket);
  }
  return map;
});
</script>

<template>
  <div class="app">
    <section class="hero">
      <div class="container hero__inner">
        <img class="hero__icon" :src="iconUrl" alt="" />
        <p class="eyebrow">SSDLC Metric Catalogue</p>
        <h1>Security metrics that map cleanly to software delivery.</h1>
        <p class="subtitle">
          Browse the phases of the SSDLC and the top-level metrics that define how security is
          measured across the lifecycle.
        </p>
        <div class="hero__actions">
          <router-link class="primary" to="/metrics">View all metrics</router-link>
          <a class="ghost" :href="proposeMetricUrl" target="_blank" rel="noopener noreferrer">
            Propose a metric
          </a>
        </div>
        <div class="hero__meta">
          <span>{{ phases.length }} phases</span>
          <span>•</span>
          <span>{{ metrics.length }} metrics</span>
          <span v-if="formattedUpdatedAt">• Updated {{ formattedUpdatedAt }}</span>
        </div>
      </div>
    </section>

    <section class="section section--light phase-section">
      <div class="container">
        <p class="eyebrow">Phase map</p>
        <div class="section-header section-header--inline">
          <h2>SSDLC phases</h2>
          <p class="section-subtitle section-subtitle--inline">
            Top-level metrics are those that are not dependencies of other metrics. Each card shows
            the metrics that define success for that phase.
          </p>
        </div>

        <div v-if="loading" class="state">Loading metrics catalogue…</div>
        <div v-else-if="error" class="state state--error">{{ error }}</div>
        <div v-else class="phase-grid">
          <router-link
            v-for="(phase, index) in phases"
            :key="phase.id"
            class="phase-card phase-card--link"
            :style="{ '--delay': index }"
            :to="`/metrics?phase=${phase.id}`"
          >
            <div class="phase-card__header">
              <h3 class="phase-card__title">{{ phase.name }}</h3>
              <div class="phase-card__icon">
                <component :is="iconMap[phase.icon] ?? fallbackIcon" :size="24" />
              </div>
            </div>
            <p class="phase-card__desc">{{ phase.description }}</p>
            <div class="phase-card__meta">
              <span class="meta-value">{{ topLevelByPhase.get(phase.id)?.length ?? 0 }}</span>
              <span class="meta-label">top-level metrics</span>
            </div>
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>
