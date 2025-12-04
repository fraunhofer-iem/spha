import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { mount, type VueWrapper } from "@vue/test-utils";
import HealthScore from "../HealthScore.vue";
import { Chart } from "chart.js";
import { Product, type Kpi } from "../../model/Result.ts";

// Mock Chart.js
vi.mock("chart.js", () => {
  const mockChart = {
    destroy: vi.fn(),
    update: vi.fn(),
    resize: vi.fn(),
  };

  const ChartConstructor = vi.fn(() => mockChart) as any;
  ChartConstructor.register = vi.fn();

  return {
    Chart: ChartConstructor,
    DoughnutController: vi.fn(),
    ArcElement: vi.fn(),
    Tooltip: vi.fn(),
    Legend: vi.fn(),
  };
});

// Mock annotation plugin
vi.mock("chartjs-plugin-annotation", () => ({
  default: {},
}));

// Mock DashboardCard component
vi.mock("../DashboardCard.vue", () => ({
  default: {
    name: "DashboardCard",
    props: ["title"],
    template:
      '<div class="dashboard-card"><slot></slot><div class="card-footer" v-if="$slots.footer"><slot name="footer"></slot></div></div>',
  },
}));

// Mock colors
vi.mock("../assets/styles/Colors.ts", () => ({
  blue_chart: "#3D4BF6",
}));

describe("HealthScore", () => {
  let wrapper: VueWrapper<any>;
  let mockChartInstance: any;

  const mockRootKpi: Kpi = {
    displayName: "Overall Health",
    score: 85,
    id: "root",
    children: [
      { displayName: "Code Quality", score: 90, id: "quality", children: [] },
      { displayName: "Security", score: 80, id: "security", children: [] },
    ],
  } as any;

  const buildProduct = (scores: number[], rootKpi: Kpi = mockRootKpi) => {
    const now = Date.now();
    const results = scores.map((s, idx) => ({
      healthScore: s,
      repoInfo: { repoLanguages: [] },
      root: rootKpi,
      tools: [],
      createdAt: new Date(now + idx * 1000).toISOString(),
    }));
    return new Product("p1", "Prod 1", results as any);
  };

  beforeEach(() => {
    mockChartInstance = {
      destroy: vi.fn(),
      update: vi.fn(),
      resize: vi.fn(),
    };
    (Chart as any).mockReturnValue(mockChartInstance);
    vi.clearAllMocks();
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
  });

  describe("Component Initialization", () => {
    it("should mount with required product prop", () => {
      wrapper = mount(HealthScore, {
        props: {
          product: buildProduct([75]),
        },
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.props().product).toBeTruthy();
    });

    it("should create chart on mount", async () => {
      wrapper = mount(HealthScore, {
        props: {
          product: buildProduct([85]),
        },
      });

      await wrapper.vm.$nextTick();

      expect(Chart).toHaveBeenCalled();
      const chartCall = (Chart as any).mock.calls[0];
      expect(chartCall[1].type).toBe("doughnut");
    });

    it("should pass correct data to chart", async () => {
      const score = 65;
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([score]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.data.datasets[0].data).toEqual([score, 100 - score]);
    });

    it("should configure chart with correct options", async () => {
      wrapper = mount(HealthScore, {
        props: {
          product: buildProduct([90]),
        },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.options.cutout).toBe("80%");
      expect(chartConfig.options.circumference).toBe(240);
      expect(chartConfig.options.rotation).toBe(-120);
      expect(chartConfig.options.responsive).toBe(true);
    });
  });

  describe("Score Display Logic", () => {
    it("should display score correctly in annotation (first and third lines)", async () => {
      const score = 42;
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([score]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];
      const annotation =
        chartConfig.options.plugins.annotation.annotations.dLabel;

      const content = annotation.content();
      expect(content[0]).toBe(`${score}/100`);
      expect(content[2]).toBe("score");
    });

    it("should handle zero score", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([0]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.data.datasets[0].data).toEqual([0, 100]);

      const annotation =
        chartConfig.options.plugins.annotation.annotations.dLabel;
      const content = annotation.content();
      expect(content[0]).toBe("0/100");
      expect(content[2]).toBe("score");
    });

    it("should handle maximum score", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([100]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.data.datasets[0].data).toEqual([100, 0]);

      const annotation =
        chartConfig.options.plugins.annotation.annotations.dLabel;
      const content = annotation.content();
      expect(content[0]).toBe("100/100");
      expect(content[2]).toBe("score");
    });

    it("should handle decimal scores", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([75.5]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.data.datasets[0].data).toEqual([75.5, 24.5]);

      const annotation =
        chartConfig.options.plugins.annotation.annotations.dLabel;
      const content = annotation.content();
      expect(content[0]).toBe("75.5/100");
      expect(content[2]).toBe("score");
    });
  });

  describe("Chart Styling and Configuration", () => {
    it("should configure chart colors correctly", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([80]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];
      const dataset = chartConfig.data.datasets[0];

      expect(dataset.borderWidth).toBe(0);

      // Test backgroundColor function
      const bgColorFunc = dataset.backgroundColor;
      expect(typeof bgColorFunc).toBe("function");

      // Test background color for progress (dataIndex 0)
      const progressColor = bgColorFunc({ dataIndex: 0 });
      expect(progressColor).toBe("#3D4BF6");

      // Test background color for remaining (dataIndex 1)
      const remainingColor = bgColorFunc({ dataIndex: 1 });
      expect(remainingColor).toBe("rgba(230, 230, 230, 0.5)");
    });

    it("should disable interactions", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([60]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.options.interaction.intersect).toBe(false);
      expect(chartConfig.options.interaction.mode).toBe(false);
      expect(chartConfig.options.hover.mode).toBeUndefined();
    });

    it("should configure annotation styling", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([55]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];
      const annotation =
        chartConfig.options.plugins.annotation.annotations.dLabel;

      expect(annotation.type).toBe("doughnutLabel");
      expect(annotation.font).toEqual([
        { size: 40, weight: "bold" },
        { size: 20, weight: "bold" },
        { size: 25 },
      ]);
      expect(annotation.color).toEqual(["black", "grey", "grey"]);
    });

    it("should hide legend", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([70]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.options.plugins.legend.display).toBe(false);
    });
  });

  describe("Chart Lifecycle Management", () => {
    it("should destroy previous chart when product (score) changes", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([50]) },
      });

      await wrapper.vm.$nextTick();

      // Change the score by replacing the product with a new one
      await wrapper.setProps({ product: buildProduct([80]) });

      expect(mockChartInstance.destroy).toHaveBeenCalled();
      expect(Chart).toHaveBeenCalledTimes(2); // Initial + after prop change
    });

    it("should destroy chart on component unmount", () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([90]) },
      });

      wrapper.unmount();

      expect(mockChartInstance.destroy).toHaveBeenCalled();
    });

    it("should handle multiple score changes correctly", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([30]) },
      });

      await wrapper.vm.$nextTick();

      // Change score multiple times via new product instances
      await wrapper.setProps({ product: buildProduct([50]) });
      await wrapper.setProps({ product: buildProduct([70]) });
      await wrapper.setProps({ product: buildProduct([90]) });

      expect(mockChartInstance.destroy).toHaveBeenCalledTimes(3);
      expect(Chart).toHaveBeenCalledTimes(4); // Initial + 3 changes
    });

    it("should not crash if canvas is not available", async () => {
      // Mock a scenario where canvas ref is null
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([75]) },
      });

      await wrapper.vm.$nextTick();
      vi.clearAllMocks();

      // Manually set canvas ref to null
      (wrapper.vm as any).chartCanvas2 = null;

      // Trigger chart render
      (wrapper.vm as any).renderChart();

      // Should not call Chart constructor when canvas is null
      expect(Chart).not.toHaveBeenCalled();
    });
  });

  describe("Component Structure", () => {
    it("should render DashboardCard with correct title", () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([85]) },
      });

      expect(
        wrapper.findComponent({ name: "DashboardCard" }).props().title,
      ).toBe("Project Health Score");
    });

    it("should render canvas element", () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([65]) },
      });

      const canvas = wrapper.find("canvas");
      expect(canvas.exists()).toBe(true);
    });

    it("should render Details button", () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([40]) },
      });

      const button = wrapper.find("button");
      expect(button.exists()).toBe(true);
      expect(button.text()).toBe("Details");
      expect(button.classes()).toContain("text-primary-emphasis");
      expect(button.classes()).toContain("fw-bold");
      expect(button.classes()).toContain("bg-primary-subtle");
    });

    it("should have correct chart container styling", () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([95]) },
      });

      const chartContainer = wrapper.find(".chart-container");
      expect(chartContainer.exists()).toBe(true);
      expect(chartContainer.classes()).toContain("chart-container");
    });
  });

  describe("Edge Cases and Error Handling", () => {
    it("should handle negative score gracefully (bounded to 0)", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([-10]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.data.datasets[0].data).toEqual([0, 100]);

      const annotation =
        chartConfig.options.plugins.annotation.annotations.dLabel;
      const content = annotation.content();
      expect(content[0]).toBe("0/100");
      expect(content[2]).toBe("score");
    });

    it("should handle score over 100 (bounded to 100)", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([120]) },
      });

      await wrapper.vm.$nextTick();

      const chartCall = (Chart as any).mock.calls[0];
      const chartConfig = chartCall[1];

      expect(chartConfig.data.datasets[0].data).toEqual([100, 0]);

      const annotation =
        chartConfig.options.plugins.annotation.annotations.dLabel;
      const content = annotation.content();
      expect(content[0]).toBe("100/100");
      expect(content[2]).toBe("score");
    });

    it("should handle chart destruction when chart instance is null", () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([75]) },
      });

      // Set chart instance to null
      (wrapper.vm as any).chartInstance = null;

      // Should not throw error on unmount
      expect(() => wrapper.unmount()).not.toThrow();
    });

    it("should not render chart if canvas ref is not available initially", async () => {
      // Create a scenario where canvas is not immediately available
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([50]) },
        attachTo: document.body,
      });

      await wrapper.vm.$nextTick();
      vi.clearAllMocks();

      // Manually clear the canvas ref and call renderChart
      (wrapper.vm as any).chartCanvas2 = null;
      (wrapper.vm as any).renderChart();

      expect(Chart).not.toHaveBeenCalled();
    });
  });

  describe("Reactivity and Watchers", () => {
    it("should re-render when product (score) changes", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([25]) },
      });

      await wrapper.vm.$nextTick();
      vi.clearAllMocks();

      // Change score by replacing product
      await wrapper.setProps({ product: buildProduct([75]) });

      expect(Chart).toHaveBeenCalled();
    });

    it("should update chart data when score changes via product", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([40]) },
      });

      await wrapper.vm.$nextTick();

      // Verify initial data
      let chartCall = (Chart as any).mock.calls[0];
      let chartConfig = chartCall[1];
      expect(chartConfig.data.datasets[0].data).toEqual([40, 60]);

      // Change score
      await wrapper.setProps({ product: buildProduct([80]) });

      // Verify updated data
      chartCall = (Chart as any).mock.calls[1]; // Second call after prop change
      chartConfig = chartCall[1];
      expect(chartConfig.data.datasets[0].data).toEqual([80, 20]);
    });

    it("should handle rapid score changes", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([10]) },
      });

      await wrapper.vm.$nextTick();

      // Rapid changes via new product instances
      const scores = [20, 30, 40, 50, 60];
      for (const score of scores) {
        await wrapper.setProps({ product: buildProduct([score]) });
      }

      // Should have destroyed and recreated chart for each change
      expect(mockChartInstance.destroy).toHaveBeenCalledTimes(scores.length);
      expect(Chart).toHaveBeenCalledTimes(scores.length + 1); // +1 for initial
    });
  });

  describe("Button Interactions", () => {
    it("should handle Details button click", async () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([88]) },
      });

      const button = wrapper.find("button");
      await button.trigger("click");

      // Since there's no specific click handler implementation in the component,
      // we just verify the button is clickable and doesn't throw errors
      expect(button.exists()).toBe(true);
    });

    it("should have correct button styling", () => {
      wrapper = mount(HealthScore, {
        props: { product: buildProduct([77]) },
      });

      const button = wrapper.find("button");
      expect(button.classes()).toContain("btn");
      expect(button.classes()).toContain("btn-lg");
      expect(button.classes()).toContain("text-primary-emphasis");
      expect(button.classes()).toContain("fw-bold");
      expect(button.classes()).toContain("bg-primary-subtle");
    });
  });
});
