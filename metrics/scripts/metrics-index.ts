import fs from "node:fs/promises";
import path from "node:path";
import matter from "gray-matter";
import { z } from "zod";

const PhaseSchema = z
  .object({
    id: z.string().min(1),
    name: z.string().min(1),
    description: z.string().min(1),
    icon: z.string().min(1),
    order: z.number().int().nonnegative(),
  })
  .strict();

const PhasesSchema = z.array(PhaseSchema).min(1);

const ThresholdSchema = z
  .object({
    name: z.string().min(1),
    value: z.union([z.string().min(1), z.number()]),
    description: z.string().min(1).optional(),
  })
  .strict();

const MetricSchema = z
  .object({
    id: z.string().min(1),
    title: z.string().min(1),
    phase: z.string().min(1),
    thresholds: z.array(ThresholdSchema).optional(),
    tags: z.array(z.string().min(1)).optional(),
    related_tools: z.array(z.string().min(1)).optional(),
    depends_on: z.array(z.string().min(1)).optional(),
    references: z.array(z.string().min(1)).optional(),
  })
  .strict();

const ignoreDirs = new Set(["templates"]);
const ignoreFiles = new Set(["README.md"]);

const toPosixPath = (value: string) => value.split(path.sep).join("/");

function buildSearchText(metric: {
  id: string;
  title: string;
  markdown: string;
  tags?: string[];
  related_tools?: string[];
}): string {
  return [
    metric.title,
    metric.id,
    metric.markdown,
    (metric.tags ?? []).join(" "),
    (metric.related_tools ?? []).join(" "),
  ]
    .join(" ")
    .toLowerCase();
}

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

function buildDependencyMaps(metrics: Array<{ id: string; depends_on?: string[] }>) {
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

async function collectMarkdownFiles(dir: string) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const files: string[] = [];

  for (const entry of entries) {
    if (entry.name.startsWith(".")) {
      continue;
    }

    const fullPath = path.join(dir, entry.name);

    if (entry.isDirectory()) {
      if (ignoreDirs.has(entry.name)) {
        continue;
      }
      files.push(...(await collectMarkdownFiles(fullPath)));
      continue;
    }

    if (entry.isFile() && entry.name.endsWith(".md")) {
      if (ignoreFiles.has(entry.name)) {
        continue;
      }
      files.push(fullPath);
    }
  }

  return files;
}

function assertUnique(values: string[], label: string) {
  const seen = new Set<string>();
  for (const value of values) {
    if (seen.has(value)) {
      throw new Error(`Duplicate ${label}: ${value}`);
    }
    seen.add(value);
  }
}

async function loadPhases(phasesPath: string) {
  const raw = await fs.readFile(phasesPath, "utf8");
  const phases = PhasesSchema.parse(JSON.parse(raw));
  assertUnique(
    phases.map((phase) => phase.id),
    "phase id",
  );
  return phases.sort((a, b) => a.order - b.order);
}

async function loadMetrics({
  repoRoot,
  metricsDir,
  phases,
}: {
  repoRoot: string;
  metricsDir: string;
  phases: Array<{
    id: string;
    name: string;
    description: string;
    icon: string;
    order: number;
  }>;
}) {
  const phaseIds = new Set(phases.map((phase) => phase.id));
  const files = await collectMarkdownFiles(metricsDir);
  const metrics: Array<Record<string, unknown>> = [];

  for (const filePath of files) {
    const raw = await fs.readFile(filePath, "utf8");
    const parsed = matter(raw);
    const attributes = MetricSchema.parse(parsed.data ?? {});
    const body = parsed.content.trim();

    if (!body) {
      throw new Error(`Metric body is empty: ${filePath}`);
    }

    if (!phaseIds.has(attributes.phase)) {
      throw new Error(
        `Unknown phase '${attributes.phase}' in ${filePath}. Expected one of: ${[...phaseIds].join(", ")}`,
      );
    }

    const metricData = {
      ...attributes,
      markdown: body,
      source_path: toPosixPath(path.relative(repoRoot, filePath)),
    };

    metrics.push({
      ...metricData,
      search_text: buildSearchText(metricData),
    });
  }

  assertUnique(
    metrics.map((metric) => metric.id as string),
    "metric id",
  );

  const metricIds = new Set(metrics.map((metric) => metric.id as string));
  for (const metric of metrics) {
    const dependsOn = (metric.depends_on as string[] | undefined) ?? [];
    for (const dependency of dependsOn) {
      if (!metricIds.has(dependency)) {
        throw new Error(`Metric '${metric.id}' depends on unknown id '${dependency}'.`);
      }
    }
  }

  return metrics.sort((a, b) => String(a.title ?? "").localeCompare(String(b.title ?? "")));
}

export async function buildMetricsIndex({
  repoRoot,
  metricsDir,
  phasesPath,
  outputDir,
  outputPath,
}: {
  repoRoot?: string;
  metricsDir?: string;
  phasesPath?: string;
  outputDir?: string;
  outputPath?: string;
} = {}) {
  const resolvedRepoRoot = repoRoot ?? process.cwd();
  const resolvedMetricsDir = metricsDir ?? path.join(resolvedRepoRoot, "metrics");
  const resolvedPhasesPath = phasesPath ?? path.join(resolvedMetricsDir, "phases.json");
  const resolvedOutputDir = outputDir ?? path.join(resolvedRepoRoot, "public", "metrics");
  const resolvedOutputPath = outputPath ?? path.join(resolvedOutputDir, "index.json");

  const phases = await loadPhases(resolvedPhasesPath);
  const metrics = await loadMetrics({
    repoRoot: resolvedRepoRoot,
    metricsDir: resolvedMetricsDir,
    phases,
  });

  // Build dependency maps at build time
  const dependencyMaps = buildDependencyMaps(metrics);

  // Enrich metrics with pre-computed dependency data
  const enrichedMetrics = metrics.map((metric) => ({
    ...metric,
    parents: dependencyMaps.parentsById.get(String(metric.id)) ?? [],
    children: dependencyMaps.childrenById.get(String(metric.id)) ?? [],
    missing_dependencies: dependencyMaps.missingDependenciesById.get(String(metric.id)) ?? [],
  }));

  const payload = {
    generated_at: new Date().toISOString(),
    phases,
    metrics: enrichedMetrics,
    dependency_cycles: dependencyMaps.cyclePaths,
  };

  await fs.mkdir(resolvedOutputDir, { recursive: true });
  await fs.writeFile(resolvedOutputPath, JSON.stringify(payload, null, 2));

  return {
    payload,
    outputPath: resolvedOutputPath,
  };
}
