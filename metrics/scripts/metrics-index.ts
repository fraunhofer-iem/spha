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

    metrics.push({
      ...attributes,
      markdown: body,
      source_path: toPosixPath(path.relative(repoRoot, filePath)),
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

  const payload = {
    generated_at: new Date().toISOString(),
    phases,
    metrics,
  };

  await fs.mkdir(resolvedOutputDir, { recursive: true });
  await fs.writeFile(resolvedOutputPath, JSON.stringify(payload, null, 2));

  return {
    payload,
    outputPath: resolvedOutputPath,
  };
}
