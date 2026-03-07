import { describe, expect, it } from "vitest";
import { getProposeMetricUrl } from "../config";

describe("getProposeMetricUrl", () => {
  it("returns the issue template link for the hardcoded repo", () => {
    expect(getProposeMetricUrl()).toBe(
      "https://github.com/fraunhofer-iem/spha/issues/new?template=metric.yml",
    );
  });
});
