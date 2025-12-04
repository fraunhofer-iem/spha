import type {Kpi} from '../model/Result.ts';
import {computed, type ComputedRef} from 'vue';

/**
 * Get all KPIs recursively from a root KPI
 */
export const getAllKpis = (kpi: Kpi): Kpi[] => {
    const result = [kpi];
    if (kpi.children && kpi.children.length > 0) {
        kpi.children.forEach((child) => {
            result.push(...getAllKpis(child));
        });
    }
    return result;
};

/**
 * Get the lowest threshold value for a KPI
 */
export const getLowestThreshold = (kpi: Kpi): number | null => {
    if (!kpi.thresholds || kpi.thresholds.length === 0) {
        return null;
    }
    return Math.min(...kpi.thresholds.map(t => t.value));
};

/**
 * Sort children KPIs - valid KPIs first, then insufficient data KPIs
 */
export const sortChildren = (children: Kpi[] | undefined) => {
    if (!children || children.length === 0) {
        return [];
    }

    const validKpis = children.filter(kpi => kpi.score !== -1);
    const insufficientDataKpis = children.filter(kpi => kpi.score === -1);

    return [...validKpis, ...insufficientDataKpis];
};

/**
 * Get badge class based on KPI score and thresholds
 */
export const getBadgeClass = (kpi: Kpi): string => {
    if (kpi.score === -1) {
        return 'bg-secondary';
    }

    const threshold = getLowestThreshold(kpi);
    const criticalThreshold = threshold !== null ? threshold - 10 : 20;
    const warningThreshold = threshold !== null ? threshold + 10 : 50;

    if (kpi.score < criticalThreshold) {
        return 'bg-danger';
    } else if (kpi.score < warningThreshold) {
        return 'bg-warning';
    } else {
        return 'bg-success';
    }
};

/**
 * Check if a KPI is in critical state
 */
export const isCriticalKpi = (kpi: Kpi): boolean => {
    if (kpi.score === -1) return false;

    const threshold = getLowestThreshold(kpi);
    const criticalThreshold = threshold !== null ? threshold - 10 : 20;
    return kpi.score < criticalThreshold;
};

/**
 * Check if a KPI is in warning state (low but not critical)
 */
export const isWarningKpi = (kpi: Kpi): boolean => {
    if (kpi.score === -1) return false;

    const threshold = getLowestThreshold(kpi);
    const criticalThreshold = threshold !== null ? threshold - 10 : 20;
    const warningThreshold = threshold !== null ? threshold + 10 : 50;

    return kpi.score >= criticalThreshold && kpi.score < warningThreshold;
};

/**
 * Check if a KPI has critical or warning values
 */
export const hasCriticalOrWarningValue = (kpi: Kpi): boolean => {
    if (kpi.score === -1) return false;

    const threshold = getLowestThreshold(kpi);
    const warningThreshold = threshold !== null ? threshold + 10 : 50;
    return kpi.score < warningThreshold;
};

/**
 * Composable for KPI filtering and categorization
 */
export const useKpiFilters = (root: ComputedRef<Kpi | undefined>) => {
    if (root.value === undefined) {
        return {
            allKpis: computed(() => []),
            nonLeafKpis: computed(() => []),
            criticalKpis: computed(() => []),
            warningKpis: computed(() => []),
            criticalAndLowKpis: computed(() => []),
            sortedKpisBySeverity: computed(() => [])
        }
    }
    const rootKpi = root.value!!
    const allKpis = computed(() => getAllKpis(rootKpi));

    // KPIs with children only (non-leaf KPIs)
    const nonLeafKpis = computed(() =>
        allKpis.value.filter(kpi => kpi.children && kpi.children.length > 0)
    );

    // Critical KPIs (excluding root, only non-leaf)
    const criticalKpis = computed(() =>
        nonLeafKpis.value.filter((kpi) => {
            if (kpi.id === rootKpi.id) return false;
            return isCriticalKpi(kpi);
        })
    );

    // Warning KPIs (excluding root, only non-leaf, excluding already critical)
    const warningKpis = computed(() =>
        nonLeafKpis.value.filter((kpi) => {
            if (kpi.id === rootKpi.id) return false;
            const isAlreadyCritical = criticalKpis.value.includes(kpi);
            return isWarningKpi(kpi) && !isAlreadyCritical;
        })
    );

    // Critical and warning KPIs combined (excluding root, only non-leaf)
    const criticalAndLowKpis = computed(() =>
        nonLeafKpis.value.filter((kpi) => {
            if (kpi.id === rootKpi.id) return false;
            return hasCriticalOrWarningValue(kpi);
        })
    );

    // Sort KPIs by severity (critical first, then warning)
    const sortedKpisBySeverity = computed(() => {
        return criticalAndLowKpis.value.sort((a, b) => {
            const isCriticalA = isCriticalKpi(a);
            const isCriticalB = isCriticalKpi(b);

            if (isCriticalA && !isCriticalB) return -1;
            if (!isCriticalA && isCriticalB) return 1;

            return a.score - b.score; // Sort by score within same severity level
        });
    });

    return {
        allKpis,
        nonLeafKpis,
        criticalKpis,
        warningKpis,
        criticalAndLowKpis,
        sortedKpisBySeverity
    };
};