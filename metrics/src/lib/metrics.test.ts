import { describe, expect, it, afterEach } from "vitest";
import { loadMetricsIndex } from "./metrics";
import { mockFetch } from "../test/utils";
import { vi } from "vitest";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("loadMetricsIndex", () => {
  it("returns parsed metrics index", async () => {
    const payload = {
      generated_at: "2024-01-01T00:00:00Z",
      phases: [
        {
          id: "plan",
          name: "Plan",
          description: "Planning",
          icon: "calendar",
          order: 1,
        },
      ],
      metrics: [
        {
          id: "metric-a",
          title: "Metric A",
          phase: "plan",
          markdown: "Body",
          source_path: "metrics/plan/metric-a.md",
        },
      ],
    };

    mockFetch(payload);
    const result = await loadMetricsIndex();
    expect(result).toEqual(payload);
  });

  it("throws when the metrics index is malformed", async () => {
    mockFetch({ invalid: true });
    await expect(loadMetricsIndex()).rejects.toThrow("Metrics index is malformed.");
  });
});
