import { describe, expect, it, beforeEach, vi } from "vitest";
import MetricDetailView from "../MetricDetailView.vue";
import { metricsIndexFixture } from "../../test/fixtures/metricsIndex";
import { mockFetch, renderWithRouter } from "../../test/utils";

const detailRoute = {
  path: "/metrics/:id",
  component: MetricDetailView,
};

const listRoute = {
  path: "/metrics",
  name: "metrics",
  component: { template: "<div />" },
};

beforeEach(() => {
  mockFetch(metricsIndexFixture);
  vi.stubEnv("VITE_REPO_URL", "https://github.com/example/metric-catalogue");
  vi.stubEnv("VITE_REPO_BRANCH", "main");
});

describe("MetricDetailView", () => {
  it("renders markdown, related metrics, and source link", async () => {
    const { findByText, getByRole } = await renderWithRouter(MetricDetailView, {
      route: "/metrics/plan-security-requirements-coverage",
      routes: [detailRoute, listRoute],
    });

    await findByText("Security Requirements Coverage");
    await findByText("Measure security requirements coverage.");

    const relatedLink = getByRole("link", { name: "Threat Model Coverage" });
    expect(relatedLink.getAttribute("href")).toBe("/metrics/plan-threat-model-coverage");

    const sourceLink = getByRole("link", {
      name: "metrics/plan-security-requirements-coverage.md",
    });
    expect(sourceLink.getAttribute("href")).toBe(
      "https://github.com/example/metric-catalogue/blob/main/metrics/plan-security-requirements-coverage.md",
    );
  });

  it("renders the dependency graph panel and removes the dependencies card", async () => {
    const { findByText, queryByText } = await renderWithRouter(MetricDetailView, {
      route: "/metrics/plan-security-requirements-coverage",
      routes: [detailRoute, listRoute],
    });

    await findByText("Dependency graph");
    expect(queryByText("Dependencies")).toBeNull();
  });

  it("renders a feedback button with a valid link when repo is configured", async () => {
    const { findByText, getByRole } = await renderWithRouter(MetricDetailView, {
      route: "/metrics/plan-security-requirements-coverage",
      routes: [detailRoute, listRoute],
    });

    await findByText("Security Requirements Coverage");

    const feedbackLink = getByRole("link", { name: "Feedback" });
    expect(feedbackLink.getAttribute("href")).toBe(
      "https://github.com/example/metric-catalogue/issues/new?" +
        "template=metric-feedback.yml&" +
        "labels=metric-feedback&" +
        "title=Feedback%3A%20%5Bplan-security-requirements-coverage%5D%20Security%20Requirements%20Coverage&" +
        "metric_id=plan-security-requirements-coverage&" +
        "metric_title=Security%20Requirements%20Coverage&" +
        "page_url=https%3A%2F%2Fgithub.com%2Fexample%2Fmetric-catalogue%2Fblob%2Fmain%2Fmetrics%2Fplan-security-requirements-coverage.md",
    );
    expect(feedbackLink.getAttribute("target")).toBe("_blank");
    expect(feedbackLink.getAttribute("rel")).toBe("noopener noreferrer");
  });

  it("renders a disabled feedback button when repo URL is missing", async () => {
    vi.stubEnv("VITE_REPO_URL", "");

    const { findByText, getByRole, queryByRole } = await renderWithRouter(MetricDetailView, {
      route: "/metrics/plan-security-requirements-coverage",
      routes: [detailRoute, listRoute],
    });

    await findByText("Security Requirements Coverage");

    expect(queryByRole("link", { name: "Feedback" })).toBeNull();

    const feedbackButton = getByRole("button", { name: "Feedback" });
    expect(feedbackButton.getAttribute("disabled")).not.toBeNull();
    expect(feedbackButton.getAttribute("title")).toBe(
      "Feedback requires repository configuration.",
    );
  });
});
