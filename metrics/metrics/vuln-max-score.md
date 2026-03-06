---
id: vuln-max-score
title: Maximum Vulnerability Score
phase: operate
tags:
  - vulnerability-management
  - risk-prioritisation
  - security-posture
  - cvss
  - epss
related_tools:
  - Snyk
  - Grype
  - Trivy
  - Nucleus Security
  - Tenable
depends_on:
  - vuln-cvss-max-score
  - vuln-epss-max-score
thresholds:
  - name: Critical Risk
    value: ">= 0.85"
    description: Composite score indicates a severe, likely-to-be-exploited vulnerability. Immediate remediation required.
  - name: High Risk
    value: ">= 0.60"
    description: Significant combined risk. Remediation should be scheduled within the high-severity SLA.
  - name: Target
    value: "< 0.40"
    description: No vulnerability presents a combination of high severity and high exploitability likelihood.
references:
  - https://www.first.org/cvss/specification-document
  - https://www.first.org/epss/
  - https://www.cisa.gov/known-exploited-vulnerabilities-catalog
---

# Description

Measures the highest composite vulnerability risk score across all unresolved vulnerabilities in a software product, by combining each vulnerability's CVSS severity rating with its EPSS exploitability probability. The result is a single normalised score (0.0–1.0) that surfaces the vulnerability posing the greatest *combined* risk — not just the most severe in theory, but the most likely to be actively exploited.

This metric addresses a common prioritisation failure: teams focusing exclusively on CVSS scores may spend effort on high-severity vulnerabilities that are never exploited in the wild, while overlooking moderate-severity CVEs with near-certain exploitation likelihood. By combining both dimensions, this metric enables more accurate, risk-based remediation prioritisation.

This is the primary vulnerability risk indicator and is intended for use in security dashboards, SLA tracking, and risk reporting.

# Measurement

**Data sources:**
- `vuln-cvss-max-score` — the highest CVSS v3.x base score across unresolved vulnerabilities
- `vuln-epss-max-score` — the highest EPSS score across unresolved vulnerabilities
- The vulnerability inventory must be consistent between both sub-metrics (same scope, same snapshot time)

**Calculation:**

The composite score is computed per vulnerability, then the maximum is taken across all unresolved findings:

```
composite_score(v) = (CVSS_score(v) / 10.0) * 0.6 + EPSS_score(v) * 0.4

vuln-max-score = MAX(composite_score(v)) over all unresolved vulnerabilities in scope
```

- `CVSS_score / 10.0` normalises CVSS to a 0.0–1.0 range
- The **60/40 weighting** reflects that severity provides the baseline risk floor, while exploitability probability modulates urgency. This weighting should be reviewed and adjusted to fit organisational risk appetite.
- The result is a single score in the range 0.0–1.0

**Example:**

| CVE | CVSS | CVSS (norm) | EPSS | Composite |
|-----|------|------------|------|-----------|
| CVE-A | 9.8 | 0.98 | 0.05 | 0.98×0.6 + 0.05×0.4 = **0.61** |
| CVE-B | 6.5 | 0.65 | 0.91 | 0.65×0.6 + 0.91×0.4 = **0.75** |
| CVE-C | 7.2 | 0.72 | 0.60 | 0.72×0.6 + 0.60×0.4 = **0.67** |

→ `vuln-max-score = 0.75` (driven by CVE-B, which has high exploitability despite moderate severity)

**Automation:** Can be implemented as a pipeline step that queries the SCA tool output and FIRST EPSS API, computes the composite score per CVE, and emits the maximum. Output should include the winning CVE ID for traceability.

# Notes

- **Weighting is configurable:** The 60/40 split is a recommended default. Organisations in high-threat environments (e.g. internet-facing products) may want to increase the EPSS weight. Document any deviation from the default.
- **Identify the worst offender:** Always report the CVE ID that produced the maximum score alongside the score itself. The raw number alone is not actionable.
- **CISA KEV as a signal:** Consider flagging any vulnerability that appears in the CISA Known Exploited Vulnerabilities (KEV) catalogue as automatically elevated, regardless of composite score — active exploitation is confirmed, not just predicted.
- **Snapshot consistency:** CVSS and EPSS data must be pulled at the same point in time and from the same vulnerability inventory. Mismatched snapshots can produce misleading results.
- **Scope alignment:** The vulnerability scope (dependencies, first-party, containers, etc.) must be identical to that defined in `vuln-cvss-max-score` and `vuln-epss-max-score`.
- **Not a replacement for full inventory metrics:** This metric tracks the *worst case*. Complementary metrics (e.g. count of critical vulnerabilities, mean time to remediate) are needed for a complete picture of vulnerability health.
