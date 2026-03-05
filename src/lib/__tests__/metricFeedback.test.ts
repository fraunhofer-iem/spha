import { afterEach, describe, expect, it, vi } from "vitest";
import { buildMetricFeedbackUrl } from "../metricFeedback";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("buildMetricFeedbackUrl", () => {
  it("builds a fully populated feedback URL", () => {
    vi.stubEnv("VITE_REPO_URL", "https://github.com/example/metric-catalogue");

    const url = buildMetricFeedbackUrl({
      id: "plan-security-requirements-coverage",
      title: "Security Requirements Coverage",
      pageUrl: "https://example.com/metrics/plan-security-requirements-coverage",
    });

    expect(url).toBe(
      "https://github.com/example/metric-catalogue/issues/new?" +
        "template=metric-feedback.yml&" +
        "labels=metric-feedback&" +
        "title=Feedback%3A%20%5Bplan-security-requirements-coverage%5D%20Security%20Requirements%20Coverage&" +
        "metric_id=plan-security-requirements-coverage&" +
        "metric_title=Security%20Requirements%20Coverage&" +
        "page_url=https%3A%2F%2Fexample.com%2Fmetrics%2Fplan-security-requirements-coverage",
    );
  });

  it("omits metric_title when missing", () => {
    vi.stubEnv("VITE_REPO_URL", "https://github.com/example/metric-catalogue");

    const url = buildMetricFeedbackUrl({ id: "metric-a" });

    expect(url).toBe(
      "https://github.com/example/metric-catalogue/issues/new?" +
        "template=metric-feedback.yml&" +
        "labels=metric-feedback&" +
        "title=Feedback%3A%20%5Bmetric-a%5D&" +
        "metric_id=metric-a",
    );
    expect(url).not.toContain("metric_title=");
    expect(url).not.toContain("page_url=");
  });

  it("encodes special characters in the title", () => {
    vi.stubEnv("VITE_REPO_URL", "https://github.com/example/metric-catalogue");

    const url = buildMetricFeedbackUrl({
      id: "metric-a",
      title: "API / auth? & #",
    });

    expect(url).toBe(
      "https://github.com/example/metric-catalogue/issues/new?" +
        "template=metric-feedback.yml&" +
        "labels=metric-feedback&" +
        "title=Feedback%3A%20%5Bmetric-a%5D%20API%20%2F%20auth%3F%20%26%20%23&" +
        "metric_id=metric-a&" +
        "metric_title=API%20%2F%20auth%3F%20%26%20%23",
    );
  });

  it("returns null when the repo URL is missing", () => {
    vi.stubEnv("VITE_REPO_URL", "");

    expect(buildMetricFeedbackUrl({ id: "metric-a" })).toBeNull();
  });
});
