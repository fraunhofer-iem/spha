import { describe, expect, it, beforeEach } from "vitest";
import OverviewView from "../OverviewView.vue";
import { metricsIndexFixture } from "../../test/fixtures/metricsIndex";
import { mockFetch, renderWithRouter } from "../../test/utils";

const overviewRoute = {
  path: "/",
  component: OverviewView,
};

const metricsRoute = {
  path: "/metrics",
  component: { template: "<div />" },
};

beforeEach(() => {
  mockFetch(metricsIndexFixture);
});

describe("OverviewView", () => {
  it("links phase cards to metrics with preselected phase", async () => {
    const { findByRole } = await renderWithRouter(OverviewView, {
      route: "/",
      routes: [overviewRoute, metricsRoute],
    });

    const planLink = await findByRole("link", { name: /Plan/i });
    expect(planLink.getAttribute("href")).toBe("/metrics?phase=plan");
  });
});
