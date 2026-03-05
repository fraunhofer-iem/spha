import { computed, onMounted, ref } from "vue";
import { loadMetricsIndex, type Metric, type MetricsIndex, type Phase } from "./metrics";
import { buildMetricSearchText } from "./metricsFilter";

const metricsIndex = ref<MetricsIndex | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
let inflight: Promise<MetricsIndex> | null = null;

async function ensureLoaded() {
  if (metricsIndex.value) return;
  if (inflight) {
    await inflight;
    return;
  }

  loading.value = true;
  error.value = null;
  inflight = loadMetricsIndex();

  try {
    metricsIndex.value = await inflight;
  } catch (err) {
    error.value = err instanceof Error ? err.message : "Failed to load metrics.";
  } finally {
    loading.value = false;
    inflight = null;
  }
}

export function useMetricsCatalogue() {
  onMounted(() => {
    void ensureLoaded();
  });

  const phases = computed<Phase[]>(() => metricsIndex.value?.phases ?? []);
  const metrics = computed<Metric[]>(() => metricsIndex.value?.metrics ?? []);
  const phaseById = computed(() => {
    const map = new Map<string, Phase>();
    for (const phase of phases.value) {
      map.set(phase.id, phase);
    }
    return map;
  });
  const phaseLabelMap = computed(() => {
    const map = new Map<string, string>();
    for (const phase of phases.value) {
      map.set(phase.id, phase.name);
    }
    return map;
  });
  const metricById = computed(() => {
    const map = new Map<string, Metric>();
    for (const metric of metrics.value) {
      map.set(metric.id, metric);
    }
    return map;
  });
  const metricsByPhase = computed(() => {
    const map = new Map<string, Metric[]>();
    for (const phase of phases.value) {
      map.set(phase.id, []);
    }
    for (const metric of metrics.value) {
      const bucket = map.get(metric.phase) ?? [];
      bucket.push(metric);
      map.set(metric.phase, bucket);
    }
    return map;
  });
  const metricSearchTextById = computed(() => {
    const map = new Map<string, string>();
    for (const metric of metrics.value) {
      map.set(metric.id, buildMetricSearchText(metric));
    }
    return map;
  });

  const formattedUpdatedAt = computed(() => {
    if (!metricsIndex.value?.generated_at) return "";
    const date = new Date(metricsIndex.value.generated_at);
    return new Intl.DateTimeFormat("en-US", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(date);
  });

  return {
    metricsIndex,
    loading,
    error,
    phases,
    metrics,
    phaseById,
    phaseLabelMap,
    metricById,
    metricsByPhase,
    metricSearchTextById,
    formattedUpdatedAt,
    reload: async () => {
      metricsIndex.value = null;
      await ensureLoaded();
    },
  };
}

export function resetMetricsCatalogue() {
  metricsIndex.value = null;
  loading.value = false;
  error.value = null;
  inflight = null;
}
