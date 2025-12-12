import type {Kpi} from "../model/Result.ts";

export interface KpiThresholdAnalysis {
    totalKpis: number;
    kpisAboveThreshold: number;
    kpisBelowThreshold: number;
    kpisAbove: Kpi[];
    kpisBelow: Kpi[];
    percentage: number;
}

/**
 * Analyzes KPIs to determine which ones are above a given threshold
 * @param rootKpi - The root KPI containing children to analyze
 * @param threshold - The threshold score (default: 60)
 * @returns Analysis of KPIs above/below threshold
 */
export function getKpisOverThreshold(
    rootKpi: Kpi,
    defaultThreshold: number = 60,
): KpiThresholdAnalysis {
    const kpis = rootKpi.children;
    const kpisAbove = kpis.filter((kpi) => {
        const threshold = getKpiThreshold(kpi, defaultThreshold);
        return kpi.score >= threshold;
    });
    const kpisBelow = kpis.filter((kpi) => {
        const threshold = getKpiThreshold(kpi, defaultThreshold);
        return kpi.score < threshold;
    });

    return {
        totalKpis: kpis.length,
        kpisAboveThreshold: kpisAbove.length,
        kpisBelowThreshold: kpisBelow.length,
        kpisAbove,
        kpisBelow,
        percentage:
            kpis.length > 0 ? Math.round((kpisAbove.length / kpis.length) * 100) : 0,
    };
}

/**
 * Gets the effective threshold for a KPI, using the smallest available threshold
 * @param kpi - The KPI to get the threshold for
 * @param defaultThreshold - Default threshold if no individual threshold is available
 * @returns The effective threshold value
 */
export function getKpiThreshold(
    kpi: Kpi,
    defaultThreshold: number = 60,
): number {
    if (!kpi.thresholds || kpi.thresholds.length === 0) {
        return defaultThreshold;
    }

    // Use the smallest threshold if multiple are available
    return Math.min(...kpi.thresholds.map((t) => t.value));
}

/**
 * Generates descriptive text based on KPI threshold analysis
 * @param analysis - The KPI threshold analysis result
 * @returns Descriptive text for the dashboard
 */
export function generateKpiSummaryText(analysis: KpiThresholdAnalysis): string {
    const {kpisAboveThreshold, totalKpis, percentage} = analysis;

    if (totalKpis === 0) {
        return "No KPIs available for analysis.";
    }

    const numberWords = [
        "zero",
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
    ];

    const kpisAboveText =
        kpisAboveThreshold <= 10
            ? numberWords[kpisAboveThreshold]
            : kpisAboveThreshold.toString();

    const totalKpisText =
        totalKpis <= 10 ? numberWords[totalKpis] : totalKpis.toString();

    let statusText: string;
    if (percentage >= 80) {
        statusText = "The project is in excellent shape.";
    } else if (percentage >= 60) {
        statusText = "The project is in good shape.";
    } else if (percentage >= 40) {
        statusText = "The project needs some attention.";
    } else {
        statusText = "The project requires immediate attention.";
    }

    const plural = totalKpis === 1 ? "KPI is" : "KPIs are";

    return `${kpisAboveText!.charAt(0).toUpperCase() + kpisAboveText!.slice(1)} out of ${totalKpisText} ${plural} above the threshold. ${statusText} Click Details below to see more.`;
}

/**
 * Gets a color indicator based on KPI performance percentage
 * @param percentage - The percentage of KPIs above threshold
 * @returns CSS class or color indicator
 */
export function getKpiStatusColor(percentage: number): string {
    if (percentage >= 80) return "text-success";
    if (percentage >= 60) return "text-primary";
    if (percentage >= 40) return "text-warning";
    return "text-danger";
}

/**
 * Debug utility to get threshold information for all KPIs
 * @param rootKpi - The root KPI containing children to analyze
 * @param defaultThreshold - Default threshold if no individual threshold is available
 * @returns Debug information about thresholds used for each KPI
 */
export function getKpiThresholdDebugInfo(
    rootKpi: Kpi,
    defaultThreshold: number = 60,
): Array<{
    id: string;
    displayName: string;
    score: number;
    effectiveThreshold: number;
    availableThresholds: Array<{ name: string; value: number }>;
    isAboveThreshold: boolean;
    usesDefault: boolean;
}> {
    return rootKpi.children.map((kpi) => {
        const effectiveThreshold = getKpiThreshold(kpi, defaultThreshold);
        const availableThresholds = kpi.thresholds || [];
        const usesDefault = !kpi.thresholds || kpi.thresholds.length === 0;

        return {
            id: kpi.id,
            displayName: kpi.displayName,
            score: kpi.score,
            effectiveThreshold,
            availableThresholds,
            isAboveThreshold: kpi.score >= effectiveThreshold,
            usesDefault,
        };
    });
}
