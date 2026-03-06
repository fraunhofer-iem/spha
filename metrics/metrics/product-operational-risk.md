---
id: product-operational-risk
title: Product Operational Risk Score
phase: operate
tags:
  - dependency-management
  - sbom
  - risk-prioritisation
  - operational-risk
related_tools:
  - Grype
  - Trivy
  - Snyk
  - CycloneDX CLI
  - OSV Scanner
depends_on:
  - dep-risk-score
thresholds:
  - name: Low Risk
    value: "< 0.30"
    description: Dependency portfolio is generally healthy. Routine maintenance cadence is sufficient.
  - name: Medium Risk
    value: "0.30 – 0.59"
    description: Some dependencies present elevated risk. Targeted upgrade planning is recommended.
  - name: High Risk
    value: "0.60 – 0.79"
    description: Significant dependency risk across the portfolio. Structured remediation programme required.
  - name: Critical Risk
    value: ">= 0.80"
    description: Dependency portfolio presents critical operational risk. Immediate escalation and remediation required.
references:
  - https://cyclonedx.org/specification/overview/
  - https://spdx.dev/
  - https://osv.dev/
  - https://libyear.com/
---

# Description

Measures the overall operational risk a software product carries as a result of its dependency portfolio, as declared in its SBOM. The score is a weighted aggregate of the individual `dep-risk-score` values for all dependencies, where **direct dependencies are weighted more heavily than transitive dependencies**.

This asymmetry reflects a deliberate design choice: direct dependencies are explicitly chosen and maintained by the product's development team, making their risk both more attributable and more actionable. Transitive dependencies are inherited indirectly and may be harder to update without upstream coordination, but still contribute to the product's overall risk surface.

The result is a single normalised score (0.0–1.0) that summarises the dependency health of the entire product and can be tracked over time, used in SLA definitions, or included in risk reports.

# Measurement

**Data sources:**
- SBOM (CycloneDX or SPDX) for the product — must include dependency type classification (direct vs. transitive) and full transitive graph
- `dep-risk-score(d)` — computed for each dependency `d` in the SBOM

**Dependency type classification:**

CycloneDX encodes direct vs. transitive relationships in the `dependencies` element of the BOM. SPDX expresses this via relationship types (`DEPENDS_ON` for direct, inferred transitivity from the dependency graph). Ensure the SBOM generation toolchain populates this information accurately.

**Calculation:**
```
weight(d) = 1.0   if d is a direct dependency
            0.5   if d is a transitive dependency

product-operational-risk = SUM(dep-risk-score(d) × weight(d))
                         / SUM(weight(d))
                         for all d in SBOM
```

This is a weighted mean of all dependency risk scores, where each score is scaled by the dependency type weight before averaging.

**Example:**

| Dependency | Type | dep-risk-score | Weight | Weighted Score |
|---|---|---|---|---|
| lib-a | Direct | 0.80 | 1.0 | 0.80 |
| lib-b | Direct | 0.30 | 1.0 | 0.30 |
| lib-c | Transitive | 0.70 | 0.5 | 0.35 |
| lib-d | Transitive | 0.20 | 0.5 | 0.10 |

```
product-operational-risk = (0.80 + 0.30 + 0.35 + 0.10) / (1.0 + 1.0 + 0.5 + 0.5)
                         = 1.55 / 3.0
                         = 0.517  →  Medium Risk
```

**Automation:** Once `dep-risk-score` is computed per dependency, the aggregation step is straightforward arithmetic over the SBOM dependency graph. The full pipeline can be run in CI/CD on each build or on a scheduled basis.

# Notes

- **SBOM quality is a prerequisite:** This metric is only as reliable as the SBOM it is based on. SBOMs must include complete transitive dependency graphs and accurate version pinning. Shallow or incomplete SBOMs will undercount risk. Validate SBOM completeness as part of the pipeline.
- **Direct/transitive weight is configurable:** The 1.0/0.5 ratio is the recommended default. A ratio of 1.0/0.0 (direct only) is a valid configuration for teams who want to focus exclusively on actionable risk; a ratio of 1.0/1.0 gives equal weight to all dependencies.
- **Large transitive trees:** Products with deep transitive dependency trees (common in JavaScript/npm ecosystems) may have hundreds of transitive dependencies. Consider reporting the metric both with and without transitive dependencies to separate the two signals.
- **Top-N breakdown:** Alongside the aggregate score, always report the top 5–10 highest `dep-risk-score` dependencies (with type, score, and primary risk driver) to make the metric actionable. A single aggregate number without attribution is difficult to act on.
- **Deduplication of shared transitive dependencies:** If the same transitive package appears via multiple dependency paths, it should be counted only once in the calculation (deduplicated by package name + version).
- **Trend tracking:** The most valuable use of this metric is as a trend over time. A rising score on a stable codebase indicates dependency neglect; a falling score after a remediation sprint validates the impact of that work.
