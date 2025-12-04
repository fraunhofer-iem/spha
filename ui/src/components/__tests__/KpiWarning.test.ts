import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import KpiWarning from "../KpiWarning.vue";
import type { Kpi } from "../../model/Result";

// Mock DashboardCard component
const DashboardCardStub = {
  name: 'DashboardCard',
  template: '<div class="dashboard-card"><slot></slot></div>',
  props: ['title']
};

describe("KpiWarning", () => {
  it("should exclude leaf nodes from warning calculations", () => {
    // Create a KPI tree with both parent and leaf nodes
    const mockKpiTree: Kpi = {
      displayName: "Root KPI",
      score: 85,
      id: "root",
      children: [
        {
          displayName: "Parent KPI 1",
          score: 45, // Warning score (< 50) but has children - should be counted
          id: "parent1",
          children: [
            {
              displayName: "Leaf KPI 1",
              score: 30, // Warning score (< 50) but is leaf - should NOT be counted
              id: "leaf1",
              children: []
            },
            {
              displayName: "Leaf KPI 2",
              score: 15, // Critical score (< 20) but is leaf - should NOT be counted
              id: "leaf2",
              children: []
            }
          ]
        },
        {
          displayName: "Parent KPI 2",
          score: 10, // Critical score (< 20) and has children - should be counted
          id: "parent2",
          children: [
            {
              displayName: "Leaf KPI 3",
              score: 70, // Good score - not warning or critical
              id: "leaf3",
              children: []
            }
          ]
        },
        {
          displayName: "Leaf KPI 4",
          score: 25, // Warning score (< 50) but is leaf - should NOT be counted
          id: "leaf4",
          children: []
        }
      ]
    };

    const wrapper = mount(KpiWarning, {
      props: {
        root: mockKpiTree
      },
      global: {
        stubs: {
          DashboardCard: DashboardCardStub
        }
      }
    });

    // Access the component's computed properties
    const vm = wrapper.vm as any;

    // Should only count Parent KPI 1 (score 45) - has children and score < 50
    expect(vm.warningKpis).toHaveLength(1);
    expect(vm.warningKpis[0].displayName).toBe("Parent KPI 1");

    // Should only count Parent KPI 2 (score 10) - has children and score < 20  
    expect(vm.criticalKpis).toHaveLength(1);
    expect(vm.criticalKpis[0].displayName).toBe("Parent KPI 2");

    // Warnings should be true since we have both warning and critical KPIs
    expect(vm.warnings).toBe(true);

    // Check that the displayed counts are correct
    const warningContent = wrapper.find('.alert-warning .warning-content');
    expect(warningContent.text()).toContain('1 KPIs have low values');

    const criticalContent = wrapper.find('.alert-danger .warning-content');
    expect(criticalContent.text()).toContain('1 KPIs have critical values');
  });

  it("should show no warnings when only leaf nodes have low scores", () => {
    const mockKpiTree: Kpi = {
      displayName: "Root KPI",
      score: 85,
      id: "root",
      children: [
        {
          displayName: "Parent KPI",
          score: 75, // Good score
          id: "parent",
          children: [
            {
              displayName: "Leaf KPI 1",
              score: 30, // Warning score but is leaf
              id: "leaf1",
              children: []
            },
            {
              displayName: "Leaf KPI 2",
              score: 15, // Critical score but is leaf
              id: "leaf2",
              children: []
            }
          ]
        }
      ]
    };

    const wrapper = mount(KpiWarning, {
      props: {
        root: mockKpiTree
      },
      global: {
        stubs: {
          DashboardCard: DashboardCardStub
        }
      }
    });

    const vm = wrapper.vm as any;

    // Should not count any warnings or critical KPIs since only leaves have low scores
    expect(vm.warningKpis).toHaveLength(0);
    expect(vm.criticalKpis).toHaveLength(0);
    expect(vm.warnings).toBe(false);

    // Should show success message
    const successAlert = wrapper.find('.alert-success');
    expect(successAlert.exists()).toBe(true);
    expect(successAlert.text()).toContain('No critical KPIs detected');
  });

  it("should not double-count KPIs in both warning and critical lists", () => {
    const mockKpiTree: Kpi = {
      displayName: "Root KPI",
      score: 85,
      id: "root",
      children: [
        {
          displayName: "Parent KPI 1",
          score: 45, // Warning score (< 50) - should be in warning only
          id: "parent1",
          children: [
            {
              displayName: "Leaf KPI 1",
              score: 30,
              id: "leaf1",
              children: []
            }
          ]
        },
        {
          displayName: "Parent KPI 2", 
          score: 15, // Critical score (< 20) - should be in critical only, NOT warning
          id: "parent2",
          children: [
            {
              displayName: "Leaf KPI 2",
              score: 70,
              id: "leaf2", 
              children: []
            }
          ]
        },
        {
          displayName: "Parent KPI 3",
          score: 35, // Warning score (< 50) - should be in warning only
          id: "parent3",
          children: [
            {
              displayName: "Leaf KPI 3",
              score: 60,
              id: "leaf3",
              children: []
            }
          ]
        }
      ]
    };

    const wrapper = mount(KpiWarning, {
      props: {
        root: mockKpiTree
      },
      global: {
        stubs: {
          DashboardCard: DashboardCardStub
        }
      }
    });

    const vm = wrapper.vm as any;

    // Should have 2 warning KPIs (Parent KPI 1 with score 45, Parent KPI 3 with score 35)
    expect(vm.warningKpis).toHaveLength(2);
    expect(vm.warningKpis.map((kpi: any) => kpi.displayName)).toContain("Parent KPI 1");
    expect(vm.warningKpis.map((kpi: any) => kpi.displayName)).toContain("Parent KPI 3");

    // Should have 1 critical KPI (Parent KPI 2 with score 15) but NOT the warning ones
    expect(vm.criticalKpis).toHaveLength(1);
    expect(vm.criticalKpis[0].displayName).toBe("Parent KPI 2");

    // Verify no overlap - critical KPIs should not include any warning KPIs
    const warningIds = vm.warningKpis.map((kpi: any) => kpi.id);
    const criticalIds = vm.criticalKpis.map((kpi: any) => kpi.id);
    const intersection = warningIds.filter((id: string) => criticalIds.includes(id));
    expect(intersection).toHaveLength(0);

    // Check displayed counts
    const warningContent = wrapper.find('.alert-warning .warning-content');
    expect(warningContent.text()).toContain('2 KPIs have low values');

    const criticalContent = wrapper.find('.alert-danger .warning-content');
    expect(criticalContent.text()).toContain('1 KPIs have critical values');
  });
});