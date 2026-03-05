import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { buildMetricsIndex } from "../metrics-index.ts";

const phasePayload = [
  {
    id: "plan",
    name: "Plan",
    description: "Planning phase",
    icon: "calendar",
    order: 1,
  },
];

async function writeMetric({
  root,
  name,
  frontmatter,
  body,
}: {
  root: string;
  name: string;
  frontmatter: string;
  body: string;
}) {
  const metricsDir = path.join(root, "metrics");
  await fs.mkdir(metricsDir, { recursive: true });
  const content = `---\n${frontmatter}\n---\n\n${body}\n`;
  await fs.writeFile(path.join(metricsDir, name), content);
}

async function createRepo() {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), "metrics-index-"));
  const metricsDir = path.join(root, "metrics");
  await fs.mkdir(metricsDir, { recursive: true });
  await fs.writeFile(path.join(metricsDir, "phases.json"), JSON.stringify(phasePayload, null, 2));
  return root;
}

test("builds an index from valid metrics", async () => {
  const root = await createRepo();

  await writeMetric({
    root,
    name: "metric-a.md",
    frontmatter: "id: metric-a\ntitle: Metric A\nphase: plan",
    body: "Metric A description.",
  });

  const { payload, outputPath } = await buildMetricsIndex({
    repoRoot: root,
    metricsDir: path.join(root, "metrics"),
    phasesPath: path.join(root, "metrics", "phases.json"),
    outputDir: path.join(root, "public", "metrics"),
    outputPath: path.join(root, "public", "metrics", "index.json"),
  });

  assert.equal(payload.metrics.length, 1);
  assert.equal(payload.metrics[0]!!.id, "metric-a");
  assert.equal(payload.metrics[0]!!.markdown, "Metric A description.");
  assert.equal(payload.metrics[0]!!.source_path, "metrics/metric-a.md");

  const written = JSON.parse(await fs.readFile(outputPath, "utf8"));
  assert.equal(written.metrics.length, 1);
});

test("fails when the metric phase is unknown", async () => {
  const root = await createRepo();

  await writeMetric({
    root,
    name: "metric-b.md",
    frontmatter: "id: metric-b\ntitle: Metric B\nphase: build",
    body: "Metric B description.",
  });

  await assert.rejects(
    () =>
      buildMetricsIndex({
        repoRoot: root,
        metricsDir: path.join(root, "metrics"),
        phasesPath: path.join(root, "metrics", "phases.json"),
        outputDir: path.join(root, "public", "metrics"),
        outputPath: path.join(root, "public", "metrics", "index.json"),
      }),
    /Unknown phase/,
  );
});

test("fails on duplicate metric ids", async () => {
  const root = await createRepo();

  await writeMetric({
    root,
    name: "metric-c.md",
    frontmatter: "id: metric-dup\ntitle: Metric C\nphase: plan",
    body: "Metric C description.",
  });

  await writeMetric({
    root,
    name: "metric-d.md",
    frontmatter: "id: metric-dup\ntitle: Metric D\nphase: plan",
    body: "Metric D description.",
  });

  await assert.rejects(
    () =>
      buildMetricsIndex({
        repoRoot: root,
        metricsDir: path.join(root, "metrics"),
        phasesPath: path.join(root, "metrics", "phases.json"),
        outputDir: path.join(root, "public", "metrics"),
        outputPath: path.join(root, "public", "metrics", "index.json"),
      }),
    /Duplicate metric id/,
  );
});

test("fails when depends_on references unknown metrics", async () => {
  const root = await createRepo();

  await writeMetric({
    root,
    name: "metric-e.md",
    frontmatter: "id: metric-e\ntitle: Metric E\nphase: plan\ndepends_on:\n  - metric-missing",
    body: "Metric E description.",
  });

  await assert.rejects(
    () =>
      buildMetricsIndex({
        repoRoot: root,
        metricsDir: path.join(root, "metrics"),
        phasesPath: path.join(root, "metrics", "phases.json"),
        outputDir: path.join(root, "public", "metrics"),
        outputPath: path.join(root, "public", "metrics", "index.json"),
      }),
    /depends on unknown id/,
  );
});

test("fails when the metric body is empty", async () => {
  const root = await createRepo();

  await writeMetric({
    root,
    name: "metric-f.md",
    frontmatter: "id: metric-f\ntitle: Metric F\nphase: plan",
    body: "",
  });

  await assert.rejects(
    () =>
      buildMetricsIndex({
        repoRoot: root,
        metricsDir: path.join(root, "metrics"),
        phasesPath: path.join(root, "metrics", "phases.json"),
        outputDir: path.join(root, "public", "metrics"),
        outputPath: path.join(root, "public", "metrics", "index.json"),
      }),
    /Metric body is empty/,
  );
});
