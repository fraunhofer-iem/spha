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
      sourcePath: "metrics/plan-security-requirements-coverage.md",
      feedbackType: "question",
    });

    expect(url).toBe(
      "https://github.com/example/metric-catalogue/issues/new?" +
        "template=metric-feedback.yml&" +
        "labels=metric-feedback%2Cfeedback-question&" +
        "title=Feedback%3A%20%5Bplan-security-requirements-coverage%5D%20Security%20Requirements%20Coverage&" +
        "metric_id=plan-security-requirements-coverage&" +
        "metric_title=Security%20Requirements%20Coverage&" +
        "page_url=https%3A%2F%2Fgithub.com%2Fexample%2Fmetric-catalogue%2Fblob%2Fmain%2Fmetrics%2Fplan-security-requirements-coverage.md&" +
        "feedback_type=Question",
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
      feedbackType: "improvement",
    });

    expect(url).toBe(
      "https://github.com/example/metric-catalogue/issues/new?" +
        "template=metric-feedback.yml&" +
        "labels=metric-feedback%2Cfeedback-improvement&" +
        "title=Feedback%3A%20%5Bmetric-a%5D%20API%20%2F%20auth%3F%20%26%20%23&" +
        "metric_id=metric-a&" +
        "metric_title=API%20%2F%20auth%3F%20%26%20%23&" +
        "feedback_type=Improvement",
    );
  });

  it("returns null when the repo URL is missing", () => {
    vi.stubEnv("VITE_REPO_URL", "");

    expect(buildMetricFeedbackUrl({ id: "metric-a" })).toBeNull();
  });
});
