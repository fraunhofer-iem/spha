import { describe, expect, it } from "vitest";
import { getProposeMetricUrl } from "../proposeMetric";

describe("getProposeMetricUrl", () => {
  it("returns the issue template link for the hardcoded repo", () => {
    expect(getProposeMetricUrl()).toBe(
      "https://github.com/janniclas/metric-catalogue/issues/new?template=metric.yml",
    );
  });
});
