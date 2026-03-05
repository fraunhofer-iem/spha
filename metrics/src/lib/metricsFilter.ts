import type { Metric } from "./metrics";

export type MetricsFilterOptions = {
  query?: string;
  phaseFilter?: Set<string>;
  tagFilter?: Set<string>;
  toolFilter?: Set<string>;
  requireThresholds?: boolean;
  requireDependencies?: boolean;
  searchTextById?: Map<string, string>;
};

export function buildMetricSearchText(metric: Metric): string {
  return [
    metric.title,
    metric.id,
    metric.markdown,
    (metric.tags ?? []).join(" "),
    (metric.related_tools ?? []).join(" "),
  ]
    .join(" ")
    .toLowerCase();
}

export function filterMetrics(metrics: Metric[], options: MetricsFilterOptions = {}): Metric[] {
  const query = options.query?.trim().toLowerCase() ?? "";
  const phaseFilter = options.phaseFilter ?? new Set();
  const tagFilter = options.tagFilter ?? new Set();
  const toolFilter = options.toolFilter ?? new Set();
  const requireThresholds = options.requireThresholds ?? false;
  const requireDependencies = options.requireDependencies ?? false;
  const searchTextById = options.searchTextById;

  return metrics.filter((metric) => {
    if (query) {
      const haystack = searchTextById?.get(metric.id) ?? buildMetricSearchText(metric);
      if (!haystack.includes(query)) {
        return false;
      }
    }

    if (phaseFilter.size > 0 && !phaseFilter.has(metric.phase)) {
      return false;
    }

    if (tagFilter.size > 0) {
      const metricTags = metric.tags ?? [];
      if (!metricTags.some((tag) => tagFilter.has(tag))) {
        return false;
      }
    }

    if (toolFilter.size > 0) {
      const metricTools = metric.related_tools ?? [];
      if (!metricTools.some((tool) => toolFilter.has(tool))) {
        return false;
      }
    }

    if (requireThresholds && !(metric.thresholds?.length ?? 0)) {
      return false;
    }

    return !(requireDependencies && !(metric.depends_on?.length ?? 0));
  });
}
