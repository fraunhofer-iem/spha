import type { Metric } from "./metrics";

export type DependencyMaps = {
  parentsById: Map<string, string[]>;
  childrenById: Map<string, string[]>;
  missingDependenciesById: Map<string, string[]>;
  cyclePaths: string[][];
};

function uniquePreserveOrder(values: string[]): string[] {
  const seen = new Set<string>();
  const result: string[] = [];
  for (const value of values) {
    if (seen.has(value)) continue;
    seen.add(value);
    result.push(value);
  }
  return result;
}

function detectDependencyCycles(parentsById: Map<string, string[]>): string[][] {
  const cycles: string[][] = [];
  const visiting = new Set<string>();
  const visited = new Set<string>();
  const stack: string[] = [];
  const indexById = new Map<string, number>();

  const visit = (id: string) => {
    visiting.add(id);
    indexById.set(id, stack.length);
    stack.push(id);

    for (const parent of parentsById.get(id) ?? []) {
      if (!parentsById.has(parent)) continue;
      if (visiting.has(parent)) {
        const startIndex = indexById.get(parent);
        if (startIndex !== undefined) {
          cycles.push(stack.slice(startIndex).concat(parent));
        }
        continue;
      }
      if (!visited.has(parent)) {
        visit(parent);
      }
    }

    stack.pop();
    indexById.delete(id);
    visiting.delete(id);
    visited.add(id);
  };

  for (const id of parentsById.keys()) {
    if (!visited.has(id)) {
      visit(id);
    }
  }

  return cycles;
}

export function buildDependencyMaps(metrics: Metric[]): DependencyMaps {
  const parentsById = new Map<string, string[]>();
  const childrenById = new Map<string, string[]>();
  const missingDependenciesById = new Map<string, string[]>();
  const metricIds = new Set(metrics.map((metric) => metric.id));

  for (const metric of metrics) {
    parentsById.set(metric.id, []);
    childrenById.set(metric.id, []);
  }

  for (const metric of metrics) {
    const dependsOn = uniquePreserveOrder(metric.depends_on ?? []);
    const parents: string[] = [];
    const missing: string[] = [];

    for (const dependency of dependsOn) {
      if (dependency === metric.id) continue;
      if (metricIds.has(dependency)) {
        parents.push(dependency);
        childrenById.get(dependency)?.push(metric.id);
      } else {
        missing.push(dependency);
      }
    }

    parentsById.set(metric.id, parents);
    if (missing.length > 0) {
      missingDependenciesById.set(metric.id, missing);
    }
  }

  const cyclePaths = detectDependencyCycles(parentsById);

  return {
    parentsById,
    childrenById,
    missingDependenciesById,
    cyclePaths,
  };
}
