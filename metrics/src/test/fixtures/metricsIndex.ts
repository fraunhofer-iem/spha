import type { MetricsIndex } from "../../lib/metrics";

export const metricsIndexFixture: MetricsIndex = {
  generated_at: "2026-02-25T00:00:00.000Z",
  phases: [
    {
      id: "plan",
      name: "Plan",
      description: "Define security goals.",
      icon: "calendar",
      order: 1,
    },
    {
      id: "code",
      name: "Code",
      description: "Implement securely.",
      icon: "code",
      order: 2,
    },
    {
      id: "build",
      name: "Build",
      description: "Build and package.",
      icon: "hammer",
      order: 3,
    },
  ],
  metrics: [
    {
      id: "plan-security-requirements-coverage",
      title: "Security Requirements Coverage",
      phase: "plan",
      tags: ["requirements", "governance"],
      related_tools: ["jira"],
      thresholds: [
        {
          name: "Coverage target",
          value: ">= 90%",
        },
      ],
      markdown: "# Description\nMeasure security requirements coverage.",
      source_path: "metrics/plan-security-requirements-coverage.md",
    },
    {
      id: "plan-threat-model-coverage",
      title: "Threat Model Coverage",
      phase: "plan",
      tags: ["requirements", "threat-modeling"],
      related_tools: ["miro"],
      depends_on: ["plan-security-requirements-coverage"],
      thresholds: [
        {
          name: "Coverage target",
          value: ">= 80%",
        },
      ],
      markdown: "# Description\nMeasure threat model coverage.",
      source_path: "metrics/plan-threat-model-coverage.md",
    },
    {
      id: "code-secure-review-coverage",
      title: "Secure Code Review Coverage",
      phase: "code",
      tags: ["code-review", "governance"],
      related_tools: ["github"],
      depends_on: ["plan-security-requirements-coverage"],
      thresholds: [
        {
          name: "Coverage target",
          value: ">= 95%",
        },
      ],
      markdown: "# Description\nMeasure secure code review coverage.",
      source_path: "metrics/code-secure-review-coverage.md",
    },
    {
      id: "build-dependency-vulnerability-rate",
      title: "Dependency Vulnerability Rate",
      phase: "build",
      tags: ["dependencies"],
      related_tools: ["snyk"],
      depends_on: ["code-secure-review-coverage"],
      thresholds: [
        {
          name: "Critical vulns target",
          value: "= 0",
        },
      ],
      markdown: "# Description\nMeasure dependency vulnerabilities.",
      source_path: "metrics/build-dependency-vulnerability-rate.md",
    },
  ],
};
