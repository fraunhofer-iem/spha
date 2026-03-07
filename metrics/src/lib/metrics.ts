import { z } from "zod";

export type Phase = {
  id: string;
  name: string;
  description: string;
  icon: string;
  order: number;
};

export type Threshold = {
  name: string;
  value: string | number;
  description?: string;
};

export type Metric = {
  id: string;
  title: string;
  phase: string;
  thresholds?: Threshold[];
  tags?: string[];
  related_tools?: string[];
  depends_on?: string[];
  references?: string[];
  markdown: string;
  source_path: string;
  search_text: string;
  parents: string[];
  children: string[];
  missing_dependencies: string[];
};

export type MetricsIndex = {
  generated_at: string;
  phases: Phase[];
  metrics: Metric[];
  dependency_cycles: string[][];
};

const PhaseSchema = z
  .object({
    id: z.string().min(1),
    name: z.string().min(1),
    description: z.string().min(1),
    icon: z.string().min(1),
    order: z.number().int().nonnegative(),
  })
  .strict();

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
    markdown: z.string().min(1),
    source_path: z.string().min(1),
    search_text: z.string().min(1),
    parents: z.array(z.string().min(1)),
    children: z.array(z.string().min(1)),
    missing_dependencies: z.array(z.string().min(1)),
  })
  .strict();

const MetricsIndexSchema = z
  .object({
    generated_at: z.string().min(1),
    phases: z.array(PhaseSchema),
    metrics: z.array(MetricSchema),
    dependency_cycles: z.array(z.array(z.string().min(1))),
  })
  .strict();

export async function loadMetricsIndex(): Promise<MetricsIndex> {
  const base = import.meta.env.BASE_URL ?? "/";
  const normalizedBase = base.endsWith("/") ? base : `${base}/`;
  const response = await fetch(`${normalizedBase}metrics/index.json`);

  if (!response.ok) {
    throw new Error(`Failed to load metrics index (${response.status})`);
  }

  const payload = await response.json();
  const parsed = MetricsIndexSchema.safeParse(payload);
  if (!parsed.success) {
    throw new Error("Metrics index is malformed.");
  }
  return parsed.data;
}
