import { spawn } from "node:child_process";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import assert from "node:assert/strict";
import test from "node:test";
import process from "node:process";
import matter from "gray-matter";

const scriptPath = path.resolve("scripts/issue-to-metric.ts");
const defaultPhases = [
  {
    id: "plan",
    name: "Plan",
    description: "Planning phase",
    icon: "calendar",
    order: 1,
  },
];

function runScript(args: string[], { cwd }: { cwd?: string } = {}) {
  return new Promise<{ code: number | null; stdout: string; stderr: string }>((resolve, reject) => {
    const child = spawn(process.execPath, ["--import", "tsx", scriptPath, ...args], {
      cwd,
      stdio: ["ignore", "pipe", "pipe"],
    });
    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (data: { toString: () => string }) => {
      stdout += data.toString();
    });

    child.stderr.on("data", (data: { toString: () => string }) => {
      stderr += data.toString();
    });

    child.on("error", reject);
    child.on("close", (code: any) => {
      resolve({ code, stdout, stderr });
    });
  });
}

function issueBody(overrides: Partial<Record<string, string>> = {}) {
  const values = {
    title: "Security: Requirements Coverage #1",
    id: "plan-security-requirements-coverage",
    phase: "plan",
    description: "Describe the metric.",
    measurement: "Measure the metric.",
    rationale: "Explain why it matters.",
    tags: "requirements, governance",
    related_tools: "jira, github",
    depends_on: "plan-threat-model-coverage",
    thresholds:
      "Coverage target | >= 90% | Percentage of epics with explicit security requirements.",
    references: "https://owasp.org/www-project-application-security-verification-standard/",
    notes: "Extra notes.",
    ...overrides,
  };

  return (
    `### Metric title\n${values.title}\n\n` +
    `### Metric id\n${values.id}\n\n` +
    `### SSDLC phase\n${values.phase}\n\n` +
    `### Description\n${values.description}\n\n` +
    `### Measurement\n${values.measurement}\n\n` +
    `### Rationale\n${values.rationale}\n\n` +
    `### Tags (comma-separated)\n${values.tags}\n\n` +
    `### Related tools (comma-separated)\n${values.related_tools}\n\n` +
    `### Depends on (comma-separated metric ids)\n${values.depends_on}\n\n` +
    `### Thresholds (one per line)\n${values.thresholds}\n\n` +
    `### References (one URL per line)\n${values.references}\n\n` +
    `### Notes\n${values.notes}\n`
  );
}

async function writePhases(metricsDir: string, phases = defaultPhases) {
  await fs.writeFile(path.join(metricsDir, "phases.json"), JSON.stringify(phases, null, 2));
}

test("issue-to-metric generates a markdown metric file", async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "metric-issue-"));
  const metricsDir = path.join(tempDir, "metrics");
  await fs.mkdir(metricsDir, { recursive: true });
  await writePhases(metricsDir);

  const issuePath = path.join(tempDir, "issue.md");
  await fs.writeFile(issuePath, issueBody());

  const { code, stdout, stderr } = await runScript([
    "--issue-file",
    issuePath,
    "--metrics-dir",
    metricsDir,
    "--issue-number",
    "123",
  ]);

  assert.equal(code, 0, stderr);
  const outputLines = stdout.trim().split("\n");
  assert.ok(outputLines.some((line) => line.startsWith("METRIC_ID=")));
  assert.ok(outputLines.some((line) => line.startsWith("METRIC_PATH=")));
  assert.ok(outputLines.some((line) => line.startsWith("BRANCH_NAME=")));

  const metricPath = path.join(metricsDir, "plan", "plan-security-requirements-coverage.md");
  const content = await fs.readFile(metricPath, "utf8");
  const parsed = matter(content);

  assert.equal(parsed.data.id, "plan-security-requirements-coverage");
  assert.equal(parsed.data.title, "Security: Requirements Coverage #1");
  assert.equal(parsed.data.phase, "plan");
  assert.ok(Array.isArray(parsed.data.tags));
  assert.ok(Array.isArray(parsed.data.related_tools));
  assert.ok(Array.isArray(parsed.data.depends_on));
  assert.ok(Array.isArray(parsed.data.thresholds));
  assert.ok(Array.isArray(parsed.data.references));
  assert.match(content, /# Description/);
  assert.match(content, /# Measurement/);
  assert.match(content, /# Rationale/);
  assert.match(content, /# Notes/);
  assert.ok(stdout.includes("BRANCH_NAME=metric/123-plan-security-requirements-coverage"));
});

test("issue-to-metric rejects invalid metric id", async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "metric-issue-"));
  const metricsDir = path.join(tempDir, "metrics");
  await fs.mkdir(metricsDir, { recursive: true });
  await writePhases(metricsDir);

  const issuePath = path.join(tempDir, "issue.md");
  await fs.writeFile(issuePath, issueBody({ id: "Invalid Id" }));

  const { code, stderr } = await runScript([
    "--issue-file",
    issuePath,
    "--metrics-dir",
    metricsDir,
    "--issue-number",
    "999",
  ]);

  assert.notEqual(code, 0);
  assert.match(stderr, /kebab-case/);
});

test("issue-to-metric rejects missing required fields", async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "metric-issue-"));
  const metricsDir = path.join(tempDir, "metrics");
  await fs.mkdir(metricsDir, { recursive: true });
  await writePhases(metricsDir);

  const issuePath = path.join(tempDir, "issue.md");
  await fs.writeFile(issuePath, issueBody({ title: "" }));

  const { code, stderr } = await runScript([
    "--issue-file",
    issuePath,
    "--metrics-dir",
    metricsDir,
    "--issue-number",
    "999",
  ]);

  assert.notEqual(code, 0);
  assert.match(stderr, /Missing required fields/);
});

test("issue-to-metric parses issue template with blank lines", async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "metric-issue-"));
  const metricsDir = path.join(tempDir, "metrics");
  await fs.mkdir(metricsDir, { recursive: true });
  await writePhases(metricsDir);

  const issueFixture = path.resolve("scripts/__tests__/issue.md");
  const issuePath = path.join(tempDir, "issue.md");
  await fs.copyFile(issueFixture, issuePath);

  const { code, stdout, stderr } = await runScript([
    "--issue-file",
    issuePath,
    "--metrics-dir",
    metricsDir,
    "--issue-number",
    "42",
  ]);

  assert.equal(code, 0, stderr);
  assert.ok(stdout.includes("METRIC_ID=test"));
  assert.ok(stdout.includes("BRANCH_NAME=metric/42-test"));

  const metricPath = path.join(metricsDir, "plan", "test.md");
  const content = await fs.readFile(metricPath, "utf8");

  assert.match(content, /id: test/);
  assert.match(content, /title: f/);
  assert.match(content, /phase: plan/);
  assert.match(content, /# Description/);
});

test("issue-to-metric rejects unknown phases", async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "metric-issue-"));
  const metricsDir = path.join(tempDir, "metrics");
  await fs.mkdir(metricsDir, { recursive: true });
  await writePhases(metricsDir);

  const issuePath = path.join(tempDir, "issue.md");
  await fs.writeFile(issuePath, issueBody({ phase: "build" }));

  const { code, stderr } = await runScript([
    "--issue-file",
    issuePath,
    "--metrics-dir",
    metricsDir,
    "--issue-number",
    "999",
  ]);

  assert.notEqual(code, 0);
  assert.match(stderr, /Unknown phase/);
});
