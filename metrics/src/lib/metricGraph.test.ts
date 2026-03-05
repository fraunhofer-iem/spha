import { describe, expect, it } from "vitest";
import { buildDependencyMaps } from "./metricGraph";
import { metricsIndexFixture } from "../test/fixtures/metricsIndex";

const baseMetrics = metricsIndexFixture.metrics;

describe("buildDependencyMaps", () => {
  it("builds parents and children maps", () => {
    const { parentsById, childrenById, missingDependenciesById, cyclePaths } =
      buildDependencyMaps(baseMetrics);

    expect(parentsById.get("plan-security-requirements-coverage")).toEqual([]);
    expect(parentsById.get("plan-threat-model-coverage")).toEqual([
      "plan-security-requirements-coverage",
    ]);
    expect(childrenById.get("plan-security-requirements-coverage")).toEqual([
      "plan-threat-model-coverage",
      "code-secure-review-coverage",
    ]);
    expect(childrenById.get("build-dependency-vulnerability-rate")).toEqual([]);
    expect(missingDependenciesById.size).toBe(0);
    expect(cyclePaths).toEqual([]);
  });

  it("filters missing dependencies and records them", () => {
    const metrics = [
      {
        id: "metric-a",
        title: "Metric A",
        phase: "plan",
        depends_on: ["metric-b", "metric-missing"],
        markdown: "# A",
        source_path: "metrics/a.md",
      },
      {
        id: "metric-b",
        title: "Metric B",
        phase: "plan",
        markdown: "# B",
        source_path: "metrics/b.md",
      },
    ];

    const { parentsById, childrenById, missingDependenciesById } = buildDependencyMaps(metrics);

    expect(parentsById.get("metric-a")).toEqual(["metric-b"]);
    expect(childrenById.get("metric-b")).toEqual(["metric-a"]);
    expect(missingDependenciesById.get("metric-a")).toEqual(["metric-missing"]);
  });

  it("detects dependency cycles", () => {
    const metrics = [
      {
        id: "metric-a",
        title: "Metric A",
        phase: "plan",
        depends_on: ["metric-b"],
        markdown: "# A",
        source_path: "metrics/a.md",
      },
      {
        id: "metric-b",
        title: "Metric B",
        phase: "plan",
        depends_on: ["metric-a"],
        markdown: "# B",
        source_path: "metrics/b.md",
      },
    ];

    const { cyclePaths } = buildDependencyMaps(metrics);

    expect(cyclePaths.length).toBe(1);
    expect(cyclePaths[0]).toEqual(["metric-a", "metric-b", "metric-a"]);
  });
});
