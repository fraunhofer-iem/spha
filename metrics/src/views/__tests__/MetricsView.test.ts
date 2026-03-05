import { describe, expect, it, beforeEach } from "vitest";
import { fireEvent } from "@testing-library/vue";
import MetricsView from "../MetricsView.vue";
import { metricsIndexFixture } from "../../test/fixtures/metricsIndex";
import { mockFetch, renderWithRouter, waitFor } from "../../test/utils";

const metricsRoute = {
  path: "/metrics",
  component: MetricsView,
};

const detailRoute = {
  path: "/metrics/:id",
  component: { template: "<div />" },
};

beforeEach(() => {
  mockFetch(metricsIndexFixture);
});

describe("MetricsView", () => {
  it("preselects phase filter from the URL and allows toggling", async () => {
    const { findAllByTestId, getByLabelText, getAllByTestId } = await renderWithRouter(
      MetricsView,
      {
        route: "/metrics?phase=plan",
        routes: [metricsRoute, detailRoute],
      },
    );

    const cards = await findAllByTestId("metric-card");
    expect(cards).toHaveLength(2);

    const planCheckbox = getByLabelText("Plan") as HTMLInputElement;
    expect(planCheckbox.checked).toBe(true);

    const codeCheckbox = getByLabelText("Code") as HTMLInputElement;
    await fireEvent.click(codeCheckbox);
    await waitFor(() => getAllByTestId("metric-card").length === 3);

    const updatedCards = getAllByTestId("metric-card");
    expect(updatedCards).toHaveLength(3);
  });

  it("searches across tools and tags", async () => {
    const { findAllByTestId, getByLabelText, getAllByTestId } = await renderWithRouter(
      MetricsView,
      {
        route: "/metrics",
        routes: [metricsRoute, detailRoute],
      },
    );

    await findAllByTestId("metric-card");

    const searchInput = getByLabelText("Search") as HTMLInputElement;
    await fireEvent.update(searchInput, "snyk");
    await waitFor(() => getAllByTestId("metric-card").length === 1);

    const filteredCards = getAllByTestId("metric-card");
    expect(filteredCards).toHaveLength(1);
  });
});
