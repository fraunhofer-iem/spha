---
id: dep-risk-score
title: Dependency Risk Score
phase: build
tags:
  - dependency-management
  - sbom
  - risk-prioritisation
related_tools:
  - Grype
  - Trivy
  - Snyk
  - OSV Scanner
  - Dependabot
  - deps.dev
  - Libraries.io
depends_on:
  - dep-libyears
  - dep-vuln-rate
  - dep-vuln-active
  - dep-maintenance-health
thresholds:
  - name: Low Risk
    value: "< 0.30"
    description: Dependency is reasonably up-to-date with low vulnerability history and no active issues.
  - name: Medium Risk
    value: "0.30 – 0.59"
    description: Dependency shows one or more elevated sub-metric signals. Upgrade should be planned.
  - name: High Risk
    value: "0.60 – 0.79"
    description: Dependency presents significant risk across multiple dimensions. Prioritise for near-term remediation.
  - name: Critical Risk
    value: ">= 0.80"
    description: Dependency is severely outdated, has a high vulnerability history, or carries active critical vulnerabilities. Immediate action required.
references:
  - https://libyear.com/
  - https://osv.dev/
  - https://www.first.org/cvss/specification-document
---

# Description

Produces a normalised composite risk score (0.0–1.0) for a single dependency by combining four independently measurable risk dimensions: how outdated the dependency version is (`dep-libyears`), how frequently the package has attracted vulnerabilities in the past year (`dep-vuln-rate`), how many vulnerabilities currently affect the pinned version (`dep-vuln-active`), and whether the project is actively maintained (`dep-maintenance-health`).

This score serves as a per-dependency risk signal that enables prioritisation of upgrade and remediation work across a product's full dependency tree. It is the primary input to the product-level `product-operational-risk` metric.

# Measurement

**Data sources:**

- `dep-libyears(d)` — fractional years of version lag
- `dep-vuln-rate(d)` — count of CVEs published against the package in the past 12 months
- `dep-vuln-active(d)` — count of unresolved vulnerabilities affecting the pinned version (severity-weighted)
- `dep-maintenance-health(d)` — composite maintenance health score (0.0–1.0, higher = healthier); inverted to a risk score before use

**Normalisation functions:**

Each sub-metric operates on a different scale and must be normalised to 0.0–1.0 before combining:

```
# Libyears: saturates at 5 years (adjust cap to organisational context)
norm_libyears(d)       = MIN(dep-libyears(d) / 5.0, 1.0)

# Vulnerability rate: saturates at 10 CVEs/year
norm_vuln_rate(d)      = MIN(dep-vuln-rate(d) / 10.0, 1.0)

# Active vulnerabilities: severity-weighted count, saturates at a weighted score of 10
severity_weighted_active(d) = (critical × 4 + high × 2 + medium × 1 + low × 0.5)
norm_vuln_active(d)    = MIN(severity_weighted_active(d) / 10.0, 1.0)

# Maintenance health: already 0.0–1.0; inverted so that poor health = higher risk
norm_maintenance(d)    = 1.0 - dep-maintenance-health(d)
```

**Composite score:**

```
dep-risk-score(d) = norm_libyears(d)    × 0.20
                  + norm_vuln_rate(d)   × 0.25
                  + norm_vuln_active(d) × 0.40
                  + norm_maintenance(d) × 0.15
```

**Default weight rationale:**

| Sub-metric               | Weight | Rationale                                                                                  |
| ------------------------ | ------ | ------------------------------------------------------------------------------------------ |
| `dep-libyears`           | 20%    | Outdatedness is a lagging indicator of risk; relevant but less urgent than active exposure |
| `dep-vuln-rate`          | 25%    | Historical frequency predicts future vulnerability likelihood                              |
| `dep-vuln-active`        | 40%    | Current unpatched exposure is the most immediate and actionable risk signal                |
| `dep-maintenance-health` | 15%    | Unmaintained projects accumulate risk over time; a forward-looking structural signal       |

**Automation:** All four sub-metrics can be computed from a CycloneDX/SPDX SBOM combined with OSV.dev, package registry APIs, and repository host APIs (GitHub/GitLab) or deps.dev. The composite can be computed in a single pipeline step after sub-metric collection.

# Notes

- **Normalisation caps are configurable:** The saturation caps (5 years, 10 CVEs/year, weighted score of 10) are sensible defaults. They should be reviewed against the actual distribution of values in your dependency inventory to avoid compressing real variation.
- **Weights are configurable:** The 20/25/40/15 split reflects a risk-first philosophy. Organisations focused on supply chain sustainability may prefer to increase the maintenance health weight.
- **Score is relative, not absolute:** A score of 0.6 does not mean a specific probability of compromise. Use it comparatively — for ranking and prioritisation across dependencies — rather than as a standalone risk statement.
- **Snapshot dependency:** All four sub-metrics must be calculated from the same SBOM snapshot and at the same point in time to ensure the composite score is internally consistent.
- **No-data handling:** If a sub-metric cannot be calculated (e.g. a private package with no registry entry, or a repository not accessible via API), exclude it from the weighted average and redistribute its weight proportionally across the remaining sub-metrics. Flag the dependency as having incomplete data.
