import { describe, it, expect, beforeAll } from "vitest";
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
import kpiResultsLarge from "../../../example/kpi-results.json";

describe("KpiService with large kpi-results.json", () => {
  let result: Result;
  let rootKpi: Kpi;

  beforeAll(() => {
    console.log("Testing KpiService with large kpi-results.json file...");
    const startTime = performance.now();
    result = parse(kpiResultsLarge)!;
    rootKpi = result.root;
    const endTime = performance.now();
    console.log(`Parsing took ${endTime - startTime} milliseconds`);
  });

  describe("getKpiThreshold", () => {
    it("should handle thresholds from real data", () => {
      rootKpi.children.forEach((kpi) => {
        const threshold = getKpiThreshold(kpi);
        expect(typeof threshold).toBe("number");
        expect(threshold).toBeGreaterThanOrEqual(0);
        expect(threshold).not.toBeNaN();
      });
    });

    it("should return smallest threshold when multiple exist", () => {
      const kpisWithMultipleThresholds = rootKpi.children.filter(
        (kpi) => kpi.thresholds && kpi.thresholds.length > 1,
      );

      kpisWithMultipleThresholds.forEach((kpi) => {
        const threshold = getKpiThreshold(kpi);
        const expectedSmallest = Math.min(
          ...kpi.thresholds!.map((t) => t.value),
        );
        expect(threshold).toBe(expectedSmallest);
      });
    });

    it("should use default for KPIs without thresholds", () => {
      const kpisWithoutThresholds = rootKpi.children.filter(
        (kpi) => !kpi.thresholds || kpi.thresholds.length === 0,
      );

      kpisWithoutThresholds.forEach((kpi) => {
        const threshold = getKpiThreshold(kpi);
        expect(threshold).toBe(60); // Default threshold
      });
    });
  });

  describe("getKpisOverThreshold", () => {
    let analysis: KpiThresholdAnalysis;

    beforeAll(() => {
      const startTime = performance.now();
      analysis = getKpisOverThreshold(rootKpi);
      const endTime = performance.now();
      console.log(`Analysis took ${endTime - startTime} milliseconds`);
    });

    it("should provide valid analysis structure", () => {
      expect(analysis).toHaveProperty("totalKpis");
      expect(analysis).toHaveProperty("kpisAboveThreshold");
      expect(analysis).toHaveProperty("kpisBelowThreshold");
      expect(analysis).toHaveProperty("kpisAbove");
      expect(analysis).toHaveProperty("kpisBelow");
      expect(analysis).toHaveProperty("percentage");

      expect(typeof analysis.totalKpis).toBe("number");
      expect(typeof analysis.kpisAboveThreshold).toBe("number");
      expect(typeof analysis.kpisBelowThreshold).toBe("number");
      expect(typeof analysis.percentage).toBe("number");
      expect(Array.isArray(analysis.kpisAbove)).toBe(true);
      expect(Array.isArray(analysis.kpisBelow)).toBe(true);
    });

    it("should have consistent counts", () => {
      expect(analysis.totalKpis).toBe(rootKpi.children.length);
      expect(analysis.kpisAboveThreshold + analysis.kpisBelowThreshold).toBe(
        analysis.totalKpis,
      );
      expect(analysis.kpisAbove.length).toBe(analysis.kpisAboveThreshold);
      expect(analysis.kpisBelow.length).toBe(analysis.kpisBelowThreshold);
    });

    it("should have valid percentage calculation", () => {
      const expectedPercentage =
        analysis.totalKpis > 0
          ? Math.round((analysis.kpisAboveThreshold / analysis.totalKpis) * 100)
          : 0;
      expect(analysis.percentage).toBe(expectedPercentage);
      expect(analysis.percentage).toBeGreaterThanOrEqual(0);
      expect(analysis.percentage).toBeLessThanOrEqual(100);
    });

    it("should correctly categorize KPIs", () => {
      analysis.kpisAbove.forEach((kpi) => {
        const threshold = getKpiThreshold(kpi);
        expect(kpi.score).toBeGreaterThanOrEqual(threshold);
      });

      analysis.kpisBelow.forEach((kpi) => {
        const threshold = getKpiThreshold(kpi);
        expect(kpi.score).toBeLessThan(threshold);
      });
    });

    it("should work with different default thresholds", () => {
      const lowThresholdAnalysis = getKpisOverThreshold(rootKpi, 10);
      const highThresholdAnalysis = getKpisOverThreshold(rootKpi, 90);

      expect(lowThresholdAnalysis.kpisAboveThreshold).toBeGreaterThanOrEqual(
        highThresholdAnalysis.kpisAboveThreshold,
      );
      expect(lowThresholdAnalysis.percentage).toBeGreaterThanOrEqual(
        highThresholdAnalysis.percentage,
      );
    });

    it("should handle edge case thresholds", () => {
      const minThresholdAnalysis = getKpisOverThreshold(rootKpi, -1);
      const maxThresholdAnalysis = getKpisOverThreshold(rootKpi, 10000);

      expect(minThresholdAnalysis.kpisAboveThreshold).toBe(analysis.totalKpis);
      expect(minThresholdAnalysis.percentage).toBe(100);
      expect(maxThresholdAnalysis.kpisBelowThreshold).toBe(analysis.totalKpis);
      expect(maxThresholdAnalysis.percentage).toBe(0);
    });
  });

  describe("generateKpiSummaryText", () => {
    it("should generate meaningful text for real data", () => {
      const analysis = getKpisOverThreshold(rootKpi);
      const text = generateKpiSummaryText(analysis);

      expect(typeof text).toBe("string");
      expect(text.length).toBeGreaterThan(10);
      expect(text).toMatch(
        /(zero|one|two|three|four|five|six|seven|eight|nine|ten|\d+).*out of.*(zero|one|two|three|four|five|six|seven|eight|nine|ten|\d+).*KPIs?.*above the threshold/i,
      );
      expect(text).toMatch(/The project (is in|needs|requires)/i);
    });

    it("should handle different performance levels appropriately", () => {
      const excellentAnalysis: KpiThresholdAnalysis = {
        totalKpis: rootKpi.children.length,
        kpisAboveThreshold: Math.ceil(rootKpi.children.length * 0.9),
        kpisBelowThreshold: Math.floor(rootKpi.children.length * 0.1),
        kpisAbove: [],
        kpisBelow: [],
        percentage: 90,
      };

      const poorAnalysis: KpiThresholdAnalysis = {
        totalKpis: rootKpi.children.length,
        kpisAboveThreshold: Math.ceil(rootKpi.children.length * 0.2),
        kpisBelowThreshold: Math.floor(rootKpi.children.length * 0.8),
        kpisAbove: [],
        kpisBelow: [],
        percentage: 20,
      };

      const excellentText = generateKpiSummaryText(excellentAnalysis);
      const poorText = generateKpiSummaryText(poorAnalysis);

      expect(excellentText).toMatch(/excellent shape/i);
      expect(poorText).toMatch(/immediate attention/i);
    });

    it("should handle large numbers correctly", () => {
      if (rootKpi.children.length > 10) {
        const analysis = getKpisOverThreshold(rootKpi);
        const text = generateKpiSummaryText(analysis);

        // For large numbers, should use digits instead of words
        expect(text).toMatch(/\d+/);
      }
    });
  });

  describe("getKpiStatusColor", () => {
    it("should return appropriate colors for different percentages", () => {
      const analysis = getKpisOverThreshold(rootKpi);
      const color = getKpiStatusColor(analysis.percentage);

      expect([
        "text-success",
        "text-primary",
        "text-warning",
        "text-danger",
      ]).toContain(color);

      // Test specific ranges
      expect(getKpiStatusColor(100)).toBe("text-success");
      expect(getKpiStatusColor(80)).toBe("text-success");
      expect(getKpiStatusColor(70)).toBe("text-primary");
      expect(getKpiStatusColor(60)).toBe("text-primary");
      expect(getKpiStatusColor(50)).toBe("text-warning");
      expect(getKpiStatusColor(40)).toBe("text-warning");
      expect(getKpiStatusColor(30)).toBe("text-danger");
      expect(getKpiStatusColor(0)).toBe("text-danger");
    });
  });

  describe("getKpiThresholdDebugInfo", () => {
    let debugInfo: ReturnType<typeof getKpiThresholdDebugInfo>;

    beforeAll(() => {
      const startTime = performance.now();
      debugInfo = getKpiThresholdDebugInfo(rootKpi);
      const endTime = performance.now();
      console.log(
        `Debug info generation took ${endTime - startTime} milliseconds`,
      );
    });

    it("should provide debug info for all child KPIs", () => {
      expect(debugInfo.length).toBe(rootKpi.children.length);
    });

    it("should have valid structure for each KPI", () => {
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

    it("should correctly identify threshold relationships", () => {
      debugInfo.forEach((info) => {
        const expectedAboveThreshold = info.score >= info.effectiveThreshold;
        expect(info.isAboveThreshold).toBe(expectedAboveThreshold);
      });
    });

    it("should provide meaningful threshold information", () => {
      debugInfo.forEach((info) => {
        expect(info.effectiveThreshold).toBeGreaterThanOrEqual(0);
        expect(info.effectiveThreshold).not.toBeNaN();

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

    it("should be consistent with threshold analysis", () => {
      const analysis = getKpisOverThreshold(rootKpi);

      const debugAboveCount = debugInfo.filter(
        (info) => info.isAboveThreshold,
      ).length;
      const debugBelowCount = debugInfo.filter(
        (info) => !info.isAboveThreshold,
      ).length;

      expect(debugAboveCount).toBe(analysis.kpisAboveThreshold);
      expect(debugBelowCount).toBe(analysis.kpisBelowThreshold);
    });
  });

  describe("Performance and scale validation", () => {
    it("should handle large data efficiently", () => {
      const iterations = 10;
      const startTime = performance.now();

      for (let i = 0; i < iterations; i++) {
        getKpisOverThreshold(rootKpi);
        getKpiThresholdDebugInfo(rootKpi);
      }

      const endTime = performance.now();
      const averageTime = (endTime - startTime) / iterations;

      console.log(`Average processing time: ${averageTime} milliseconds`);

      // Should process efficiently (less than 50ms average for large data)
      expect(averageTime).toBeLessThan(50);
    });

    it("should maintain consistency across multiple runs", () => {
      const analysis1 = getKpisOverThreshold(rootKpi);
      const analysis2 = getKpisOverThreshold(rootKpi);
      const debugInfo1 = getKpiThresholdDebugInfo(rootKpi);
      const debugInfo2 = getKpiThresholdDebugInfo(rootKpi);

      expect(analysis1).toEqual(analysis2);
      expect(debugInfo1).toEqual(debugInfo2);
    });
  });

  describe("Data statistics and insights", () => {
    it("should provide meaningful statistics about the data", () => {
      const analysis = getKpisOverThreshold(rootKpi);
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);

      console.log(`Total KPIs analyzed: ${analysis.totalKpis}`);
      console.log(
        `KPIs above threshold: ${analysis.kpisAboveThreshold} (${analysis.percentage}%)`,
      );
      console.log(`KPIs below threshold: ${analysis.kpisBelowThreshold}`);

      const kpisWithCustomThresholds = debugInfo.filter(
        (info) => !info.usesDefault,
      );
      const kpisWithDefaultThreshold = debugInfo.filter(
        (info) => info.usesDefault,
      );

      console.log(
        `KPIs with custom thresholds: ${kpisWithCustomThresholds.length}`,
      );
      console.log(
        `KPIs using default threshold: ${kpisWithDefaultThreshold.length}`,
      );

      const scores = debugInfo.map((info) => info.score);
      const minScore = Math.min(...scores);
      const maxScore = Math.max(...scores);
      const avgScore =
        scores.reduce((sum, score) => sum + score, 0) / scores.length;

      console.log(
        `Score range: ${minScore} - ${maxScore}, Average: ${avgScore.toFixed(2)}`,
      );

      expect(analysis.totalKpis).toBeGreaterThan(0);
    });

    it("should handle diverse threshold configurations", () => {
      const debugInfo = getKpiThresholdDebugInfo(rootKpi);

      const thresholdValues = debugInfo
        .filter((info) => !info.usesDefault)
        .flatMap((info) => info.availableThresholds.map((t) => t.value));

      if (thresholdValues.length > 0) {
        const uniqueThresholds = new Set(thresholdValues);
        console.log(
          `Unique threshold values found: ${Array.from(uniqueThresholds)
            .sort((a, b) => a - b)
            .join(", ")}`,
        );

        expect(uniqueThresholds.size).toBeGreaterThan(0);
      }
    });
  });

  describe("Integration with text generation", () => {
    it("should provide consistent user experience", () => {
      const analysis = getKpisOverThreshold(rootKpi);
      const text = generateKpiSummaryText(analysis);
      const color = getKpiStatusColor(analysis.percentage);

      // Text and color should be aligned
      if (analysis.percentage >= 80) {
        expect(text).toMatch(/excellent/i);
        expect(color).toBe("text-success");
      } else if (analysis.percentage >= 60) {
        expect(text).toMatch(/good/i);
        expect(color).toBe("text-primary");
      } else if (analysis.percentage >= 40) {
        expect(text).toMatch(/attention/i);
        expect(color).toBe("text-warning");
      } else {
        expect(text).toMatch(/immediate/i);
        expect(color).toBe("text-danger");
      }
    });
  });
});
