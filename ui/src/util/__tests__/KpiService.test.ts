import { describe, it, expect, beforeEach } from "vitest";
import {
  getKpisOverThreshold,
  getKpiThreshold,
  generateKpiSummaryText,
  getKpiStatusColor,
  getKpiThresholdDebugInfo,
  type KpiThresholdAnalysis,
} from "../KpiService";
import { parse } from "../Parser";
import type { Result, Kpi } from "../../model/Result";
import kpiResultsSmall from "../../../example/kpi-results-small.json";

describe("KpiService", () => {
  let result: Result;
  let rootKpi: Kpi;

  beforeEach(() => {
    result = parse(kpiResultsSmall)!;
    rootKpi = result.root;
  });

  describe("getKpiThreshold", () => {
    it("should return the smallest threshold when multiple thresholds exist", () => {
      const codeQualityKpi = rootKpi.children.find(
        (kpi) => kpi.displayName === "Code Quality",
      )!;

      // Code Quality has thresholds: acceptable=60, good=75
      const threshold = getKpiThreshold(codeQualityKpi);
      expect(threshold).toBe(60); // Should return the smaller one
    });

    it("should return the single threshold when only one exists", () => {
      const securityKpi = rootKpi.children.find(
        (kpi) => kpi.displayName === "Security",
      )!;
      const vulnerabilitiesKpi = securityKpi.children.find(
        (kpi) => kpi.displayName === "Known Vulnerabilities",
      )!;

      // Vulnerabilities has threshold: critical=95
      const threshold = getKpiThreshold(vulnerabilitiesKpi);
      expect(threshold).toBe(95);
    });

    it("should return default threshold when no thresholds exist", () => {
      const kpiWithoutThresholds: Kpi = {
        id: "test-kpi",
        displayName: "Test KPI",
        score: 50,
        children: [],
        thresholds: undefined,
      };

      const threshold = getKpiThreshold(kpiWithoutThresholds);
      expect(threshold).toBe(60); // Default threshold
    });

    it("should return custom default threshold", () => {
      const kpiWithoutThresholds: Kpi = {
        id: "test-kpi",
        displayName: "Test KPI",
        score: 50,
        children: [],
        thresholds: [],
      };

      const threshold = getKpiThreshold(kpiWithoutThresholds, 80);
      expect(threshold).toBe(80);
    });
  });

  describe("getKpisOverThreshold", () => {
    it("should correctly analyze KPIs with default threshold", () => {
      const analysis = getKpisOverThreshold(rootKpi);

      expect(analysis.totalKpis).toBe(5);
      expect(analysis.kpisAboveThreshold + analysis.kpisBelowThreshold).toBe(5);
      expect(analysis.kpisAbove).toHaveLength(analysis.kpisAboveThreshold);
      expect(analysis.kpisBelow).toHaveLength(analysis.kpisBelowThreshold);
      expect(analysis.percentage).toBe(
        Math.round((analysis.kpisAboveThreshold / analysis.totalKpis) * 100),
      );
    });

    it("should correctly identify KPIs above and below thresholds", () => {
      const analysis = getKpisOverThreshold(rootKpi);

      // Based on small JSON data:
      // Code Quality: 78 >= 60 (acceptable) ✓
      // Test Coverage: 86 >= 70 (minimum) ✓
      // Security: 91 >= 80 (acceptable) ✓
      // Maintainability: 55 >= 50 (minimum) ✓
      // Performance: 77 >= 70 (acceptable) ✓

      expect(analysis.kpisAboveThreshold).toBe(5);
      expect(analysis.kpisBelowThreshold).toBe(0);
      expect(analysis.percentage).toBe(100);

      const kpiNames = analysis.kpisAbove.map((kpi) => kpi.displayName);
      expect(kpiNames).toContain("Code Quality");
      expect(kpiNames).toContain("Test Coverage");
      expect(kpiNames).toContain("Security");
      expect(kpiNames).toContain("Maintainability");
      expect(kpiNames).toContain("Performance");
    });

    it("should work with custom threshold", () => {
      const analysis = getKpisOverThreshold(rootKpi, 80);

      // The custom threshold (80) is only used for KPIs WITHOUT thresholds
      // Since all KPIs in small JSON have their own thresholds, they use their smallest threshold:
      // Code Quality: 78 >= 60 (smallest of [60, 75]) ✓
      // Test Coverage: 86 >= 70 (smallest of [70, 80]) ✓
      // Security: 91 >= 80 (smallest of [80, 90]) ✓
      // Maintainability: 55 >= 50 (smallest of [50, 70]) ✓
      // Performance: 77 >= 70 (smallest of [70, 85]) ✓

      expect(analysis.kpisAboveThreshold).toBe(5);
      expect(analysis.kpisBelowThreshold).toBe(0);
      expect(analysis.percentage).toBe(100);
    });

    it("should use custom threshold for KPIs without thresholds", () => {
      // Create a modified root KPI with children that have no thresholds
      const modifiedRoot: Kpi = {
        ...rootKpi,
        children: rootKpi.children.map((child) => ({
          ...child,
          thresholds: undefined, // Remove all thresholds
        })),
      };

      const analysis = getKpisOverThreshold(modifiedRoot, 80);

      // Now with custom threshold 80:
      // Code Quality: 78 < 80 ✗
      // Test Coverage: 86 >= 80 ✓
      // Security: 91 >= 80 ✓
      // Maintainability: 55 < 80 ✗
      // Performance: 77 < 80 ✗

      expect(analysis.kpisAboveThreshold).toBe(2);
      expect(analysis.kpisBelowThreshold).toBe(3);
      expect(analysis.percentage).toBe(40);

      const kpiNamesAbove = analysis.kpisAbove.map((kpi) => kpi.displayName);
      expect(kpiNamesAbove).toContain("Test Coverage");
      expect(kpiNamesAbove).toContain("Security");
      expect(kpiNamesAbove).not.toContain("Code Quality");
      expect(kpiNamesAbove).not.toContain("Maintainability");
      expect(kpiNamesAbove).not.toContain("Performance");
    });

    it("should handle empty children array", () => {
      const emptyKpi: Kpi = {
        id: "empty",
        displayName: "Empty KPI",
        score: 100,
        children: [],
      };

      const analysis = getKpisOverThreshold(emptyKpi);

      expect(analysis.totalKpis).toBe(0);
      expect(analysis.kpisAboveThreshold).toBe(0);
      expect(analysis.kpisBelowThreshold).toBe(0);
      expect(analysis.percentage).toBe(0);
      expect(analysis.kpisAbove).toEqual([]);
      expect(analysis.kpisBelow).toEqual([]);
    });
  });

  describe("generateKpiSummaryText", () => {
    it("should generate correct text for excellent performance", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 5,
        kpisAboveThreshold: 5,
        kpisBelowThreshold: 0,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 100,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toContain("Five out of five KPIs are above the threshold");
      expect(text).toContain("The project is in excellent shape");
    });

    it("should generate correct text for good performance", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 5,
        kpisAboveThreshold: 3,
        kpisBelowThreshold: 2,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 60,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toContain("Three out of five KPIs are above the threshold");
      expect(text).toContain("The project is in good shape");
    });

    it("should generate correct text for needs attention", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 5,
        kpisAboveThreshold: 2,
        kpisBelowThreshold: 3,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 40,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toContain("Two out of five KPIs are above the threshold");
      expect(text).toContain("The project needs some attention");
    });

    it("should generate correct text for requires immediate attention", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 5,
        kpisAboveThreshold: 1,
        kpisBelowThreshold: 4,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 20,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toContain("One out of five KPIs are above the threshold");
      expect(text).toContain("The project requires immediate attention");
    });

    it("should handle single KPI correctly", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 1,
        kpisAboveThreshold: 1,
        kpisBelowThreshold: 0,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 100,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toContain("One out of one KPI is above the threshold");
    });

    it("should handle large numbers correctly", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 15,
        kpisAboveThreshold: 12,
        kpisBelowThreshold: 3,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 80,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toContain("12 out of 15 KPIs are above the threshold");
      expect(text).toContain("The project is in excellent shape");
    });

    it("should handle no KPIs", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 0,
        kpisAboveThreshold: 0,
        kpisBelowThreshold: 0,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 0,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toBe("No KPIs available for analysis.");
    });

    it("should handle zero KPIs above threshold", () => {
      const analysis: KpiThresholdAnalysis = {
        totalKpis: 3,
        kpisAboveThreshold: 0,
        kpisBelowThreshold: 3,
        kpisAbove: [],
        kpisBelow: [],
        percentage: 0,
      };

      const text = generateKpiSummaryText(analysis);
      expect(text).toContain("Zero out of three KPIs are above the threshold");
      expect(text).toContain("The project requires immediate attention");
    });
  });

  describe("getKpiStatusColor", () => {
    it("should return text-success for excellent performance (≥80%)", () => {
      expect(getKpiStatusColor(100)).toBe("text-success");
      expect(getKpiStatusColor(90)).toBe("text-success");
      expect(getKpiStatusColor(80)).toBe("text-success");
    });

    it("should return text-primary for good performance (60-79%)", () => {
      expect(getKpiStatusColor(79)).toBe("text-primary");
      expect(getKpiStatusColor(70)).toBe("text-primary");
      expect(getKpiStatusColor(60)).toBe("text-primary");
    });

    it("should return text-warning for needs attention (40-59%)", () => {
      expect(getKpiStatusColor(59)).toBe("text-warning");
      expect(getKpiStatusColor(50)).toBe("text-warning");
      expect(getKpiStatusColor(40)).toBe("text-warning");
    });

    it("should return text-danger for poor performance (<40%)", () => {
      expect(getKpiStatusColor(39)).toBe("text-danger");
      expect(getKpiStatusColor(20)).toBe("text-danger");
      expect(getKpiStatusColor(0)).toBe("text-danger");
    });

    it("should handle edge cases", () => {
      expect(getKpiStatusColor(79.9)).toBe("text-primary");
      expect(getKpiStatusColor(80.1)).toBe("text-success");
    });
  });

  describe("getKpiThresholdDebugInfo", () => {
    it("should provide comprehensive debug information", () => {
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);

      expect(debugInfo).toHaveLength(5);

      // Check structure of debug info
      debugInfo.forEach((info) => {
        expect(info).toHaveProperty("id");
        expect(info).toHaveProperty("displayName");
        expect(info).toHaveProperty("score");
        expect(info).toHaveProperty("effectiveThreshold");
        expect(info).toHaveProperty("availableThresholds");
        expect(info).toHaveProperty("isAboveThreshold");
        expect(info).toHaveProperty("usesDefault");

        expect(typeof info.id).toBe("string");
        expect(typeof info.displayName).toBe("string");
        expect(typeof info.score).toBe("number");
        expect(typeof info.effectiveThreshold).toBe("number");
        expect(Array.isArray(info.availableThresholds)).toBe(true);
        expect(typeof info.isAboveThreshold).toBe("boolean");
        expect(typeof info.usesDefault).toBe("boolean");
      });
    });

    it("should correctly identify KPIs using custom thresholds", () => {
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);

      const codeQualityInfo = debugInfo.find(
        (info) => info.displayName === "Code Quality",
      )!;
      expect(codeQualityInfo.usesDefault).toBe(false);
      expect(codeQualityInfo.effectiveThreshold).toBe(60); // Smallest of [60, 75]
      expect(codeQualityInfo.availableThresholds).toHaveLength(2);
      expect(codeQualityInfo.isAboveThreshold).toBe(true); // 78 >= 60
    });

    it("should correctly identify threshold relationships", () => {
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);

      debugInfo.forEach((info) => {
        const expectedAboveThreshold = info.score >= info.effectiveThreshold;
        expect(info.isAboveThreshold).toBe(expectedAboveThreshold);
      });
    });

    it("should work with custom default threshold", () => {
      const debugInfo = getKpiThresholdDebugInfo(rootKpi, 90);

      // Find a KPI that would use the default threshold
      const kpiUsingDefault = debugInfo.find((info) => info.usesDefault);

      if (kpiUsingDefault) {
        expect(kpiUsingDefault.effectiveThreshold).toBe(90);
      }
    });

    it("should handle available thresholds correctly", () => {
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);

      debugInfo.forEach((info) => {
        if (!info.usesDefault) {
          expect(info.availableThresholds.length).toBeGreaterThan(0);
          info.availableThresholds.forEach((threshold) => {
            expect(threshold).toHaveProperty("name");
            expect(threshold).toHaveProperty("value");
            expect(typeof threshold.name).toBe("string");
            expect(typeof threshold.value).toBe("number");
          });
        }
      });
    });
  });

  describe("Integration tests with small JSON data", () => {
    it("should provide consistent analysis across all functions", () => {
      const analysis = getKpisOverThreshold(rootKpi);
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);
      const summaryText = generateKpiSummaryText(analysis);
      const statusColor = getKpiStatusColor(analysis.percentage);

      // Consistency checks
      expect(debugInfo).toHaveLength(analysis.totalKpis);
      expect(debugInfo.filter((info) => info.isAboveThreshold)).toHaveLength(
        analysis.kpisAboveThreshold,
      );
      expect(debugInfo.filter((info) => !info.isAboveThreshold)).toHaveLength(
        analysis.kpisBelowThreshold,
      );

      // Text should be meaningful
      expect(summaryText).toBeTruthy();
      expect(summaryText.length).toBeGreaterThan(10);

      // Color should be appropriate
      expect([
        "text-success",
        "text-primary",
        "text-warning",
        "text-danger",
      ]).toContain(statusColor);
    });

    it("should handle edge cases gracefully", () => {
      // Test with modified KPI that has no thresholds
      const modifiedRoot: Kpi = {
        ...rootKpi,
        children: rootKpi.children.map((child) => ({
          ...child,
          thresholds: undefined,
        })),
      };

      const analysis = getKpisOverThreshold(modifiedRoot);
      const debugInfo = getKpiThresholdDebugInfo(modifiedRoot);

      expect(analysis.totalKpis).toBe(5);
      expect(debugInfo.every((info) => info.usesDefault)).toBe(true);
      expect(debugInfo.every((info) => info.effectiveThreshold === 60)).toBe(
        true,
      );
    });
  });

  describe("Performance tests", () => {
    it("should handle large KPI hierarchies efficiently", () => {
      const startTime = performance.now();

      // Run all main functions
      const analysis = getKpisOverThreshold(rootKpi);
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);
      generateKpiSummaryText(analysis);
      getKpiStatusColor(analysis.percentage);

      const endTime = performance.now();
      const duration = endTime - startTime;

      // Should complete quickly (less than 10ms for small data)
      expect(duration).toBeLessThan(10);
      expect(analysis).toBeDefined();
      expect(debugInfo).toBeDefined();
    });
  });
});
