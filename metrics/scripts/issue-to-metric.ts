import fs from "node:fs/promises";
import path from "node:path";
import YAML from "yaml";

const SECTION_REGEX = /(?:^|\r?\n)###\s+(.+)\r?\n([\s\S]*?)(?=\r?\n###\s+|$)/g;
const ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

function parseSections(body: string) {
  const sections: Record<string, string> = {};
  for (const match of body.matchAll(SECTION_REGEX)) {
    const label = match[1]?.trim();
    if (!label) continue;
    const value = (match[2] ?? "").trim();
    sections[label] = value === "_No response_" ? "" : value;
  }
  return sections;
}

function parseList(value: string) {
  if (!value) return [];
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseLines(value: string) {
  if (!value) return [];
  return value
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
}

function parseThresholds(value: string) {
  const lines = parseLines(value);
  return lines.map((line) => {
    const [name, val, desc] = line.split("|").map((part) => part.trim());
    if (!name || !val) {
      throw new Error(`Invalid threshold line: ${line}`);
    }
    return {
      name,
      value: val,
      description: desc || undefined,
    };
  });
}

function ensureId(id: string) {
  if (!id || !ID_PATTERN.test(id)) {
    throw new Error("Metric id must be kebab-case (lowercase letters, numbers, hyphens).");
  }
}

function buildFrontmatter(data: {
  id: string;
  title: string;
  phase: string;
  tags: string[];
  related_tools: string[];
  depends_on: string[];
  thresholds: Array<{ name: string; value: string; description?: string }>;
  references: string[];
}) {
  const frontmatter: Record<string, unknown> = {
    id: data.id,
    title: data.title,
    phase: data.phase,
  };

  if (data.tags.length) {
    frontmatter.tags = data.tags;
  }

  if (data.related_tools.length) {
    frontmatter.related_tools = data.related_tools;
  }

  if (data.depends_on.length) {
    frontmatter.depends_on = data.depends_on;
  }

  if (data.thresholds.length) {
    frontmatter.thresholds = data.thresholds.map((threshold) => ({
      name: threshold.name,
      value: threshold.value,
      ...(threshold.description ? { description: threshold.description } : {}),
    }));
  }

  if (data.references.length) {
    frontmatter.references = data.references;
  }

  const yaml = YAML.stringify(frontmatter, {
    indent: 2,
    lineWidth: 0,
  }).trimEnd();

  return `---\n${yaml}\n---`;
}

function buildBody(data: {
  description: string;
  measurement?: string;
  rationale?: string;
  notes?: string;
}) {
  const sections = ["# Description", data.description || "TBD"];

  if (data.measurement) {
    sections.push("", "# Measurement", data.measurement);
  }

  if (data.rationale) {
    sections.push("", "# Rationale", data.rationale);
  }

  if (data.notes) {
    sections.push("", "# Notes", data.notes);
  }

  return sections.join("\n");
}

async function loadPhaseIds(phasesFile: string) {
  const raw = await fs.readFile(phasesFile, "utf8");
  const parsed = JSON.parse(raw);

  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new Error(`Invalid phases file: ${phasesFile}`);
  }

  const ids = parsed
    .map((phase) => (phase && typeof phase.id === "string" ? phase.id.trim() : ""))
    .filter(Boolean);

  if (ids.length !== parsed.length) {
    throw new Error(`Invalid phases file: ${phasesFile}`);
  }

  return new Set(ids);
}

async function main() {
  const args = process.argv.slice(2);
  const issueFileIndex = args.indexOf("--issue-file");
  const metricsDirIndex = args.indexOf("--metrics-dir");
  const issueNumberIndex = args.indexOf("--issue-number");
  const phasesFileIndex = args.indexOf("--phases-file");

  if (issueFileIndex === -1) {
    throw new Error("--issue-file is required");
  }

  const issueFile = args[issueFileIndex + 1];
  const metricsDir = metricsDirIndex === -1 ? "metrics" : args[metricsDirIndex + 1];
  const issueNumber = issueNumberIndex === -1 ? "unknown" : args[issueNumberIndex + 1];
  const phasesFile =
    phasesFileIndex === -1 ? path.join(metricsDir, "phases.json") : args[phasesFileIndex + 1];

  const body = await fs.readFile(issueFile, "utf8");
  const sections = parseSections(body);

  const data = {
    title: sections["Metric title"] || "",
    id: sections["Metric id"] || "",
    phase: sections["SSDLC phase"] || "",
    description: sections["Description"] || "",
    measurement: sections["Measurement"] || "",
    rationale: sections["Rationale"] || "",
    notes: sections["Notes"] || "",
    tags: parseList(sections["Tags (comma-separated)"] || ""),
    related_tools: parseList(sections["Related tools (comma-separated)"] || ""),
    depends_on: parseList(sections["Depends on (comma-separated metric ids)"] || ""),
    thresholds: parseThresholds(sections["Thresholds (one per line)"] || ""),
    references: parseLines(sections["References (one URL per line)"] || ""),
  };

  if (!data.title || !data.id || !data.phase || !data.description) {
    throw new Error("Missing required fields: title, id, phase, description.");
  }

  ensureId(data.id);
  const phaseIds = await loadPhaseIds(phasesFile);
  if (!phaseIds.has(data.phase)) {
    throw new Error(`Unknown phase '${data.phase}'. Expected one of: ${[...phaseIds].join(", ")}`);
  }

  const fileName = `${data.id}.md`;
  const phaseDir = data.phase;
  const filePath = path.join(metricsDir, phaseDir, fileName);
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  try {
    await fs.access(filePath);
    throw new Error(`Metric file already exists: ${fileName}`);
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== "ENOENT") {
      throw err;
    }
  }

  const frontmatter = buildFrontmatter(data);
  const bodyContent = buildBody(data);
  const content = `${frontmatter}\n\n${bodyContent}\n`;

  await fs.writeFile(filePath, content, "utf8");

  const branchName = `metric/${issueNumber}-${data.id}`;

  console.log(`METRIC_ID=${data.id}`);
  console.log(`METRIC_PATH=${filePath}`);
  console.log(`BRANCH_NAME=${branchName}`);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
});
