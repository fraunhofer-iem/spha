import { describe, expect, it } from "vitest";
import { filterMetrics, buildMetricSearchText } from "./metricsFilter";
import { metricsIndexFixture } from "../test/fixtures/metricsIndex";

const baseMetrics = metricsIndexFixture.metrics;

const extraMetric = {
  id: "plan-no-thresholds",
  title: "No Thresholds Metric",
  phase: "plan",
  tags: ["misc"],
  related_tools: ["custom"],
  markdown: "# Description\nNo thresholds.",
  source_path: "metrics/plan-no-thresholds.md",
};

const metrics = [...baseMetrics, extraMetric];

function buildSearchMap() {
  const map = new Map<string, string>();
  for (const metric of metrics) {
    map.set(metric.id, buildMetricSearchText(metric));
  }
  return map;
}

describe("filterMetrics", () => {
  it("filters by phase, tags, and tools", () => {
    const result = filterMetrics(metrics, {
      phaseFilter: new Set(["plan"]),
      tagFilter: new Set(["requirements"]),
      toolFilter: new Set(["jira"]),
    });

    expect(result.map((metric) => metric.id)).toEqual(["plan-security-requirements-coverage"]);
  });

  it("filters by search query using precomputed map", () => {
    const result = filterMetrics(metrics, {
      query: "snyk",
      searchTextById: buildSearchMap(),
    });

    expect(result.map((metric) => metric.id)).toEqual(["build-dependency-vulnerability-rate"]);
  });

  it("filters by thresholds and dependencies", () => {
    const withThresholds = filterMetrics(metrics, { requireThresholds: true });
    expect(withThresholds.length).toBe(baseMetrics.length);

    const withDependencies = filterMetrics(metrics, { requireDependencies: true });
    expect(withDependencies.map((metric) => metric.id)).toEqual([
      "plan-threat-model-coverage",
      "code-secure-review-coverage",
      "build-dependency-vulnerability-rate",
    ]);
  });
});
