---
id: vuln-cvss-max-score
title: Maximum Vulnerability CVSS Score
phase: operate
tags:
  - vulnerability-management
  - cvss
  - security-posture
related_tools:
  - Dependabot
  - Snyk
  - Grype
  - Trivy
  - Black Duck
depends_on: []
thresholds:
  - name: Critical
    value: ">= 9.0"
    description: CVSS v3.1 Critical severity. Immediate remediation required.
  - name: High
    value: ">= 7.0"
    description: CVSS v3.1 High severity. Remediation required within defined SLA.
  - name: Target
    value: "< 7.0"
    description: No unresolved High or Critical CVSS-rated vulnerabilities present.
references:
  - https://www.first.org/cvss/specification-document
  - https://nvd.nist.gov/vuln-metrics/cvss
---

# Description

Measures the highest CVSS (Common Vulnerability Scoring System) score across all known, unresolved vulnerabilities in a software product. CVSS provides a standardised, vendor-neutral severity rating on a scale of 0.0–10.0, capturing characteristics such as attack vector, attack complexity, privileges required, and impact on confidentiality, integrity, and availability.

Tracking the maximum CVSS score gives a quick indicator of the worst-case severity exposure of a product at any point in time. It is used as a sub-metric to compute the composite Maximum Vulnerability Score (`vuln-max-score`), which also incorporates exploitability likelihood via the EPSS score.

> **Note:** This metric should always be evaluated against CVSS v3.1 or later. CVSS v2 scores are not comparable and should be re-scored or excluded.

# Measurement

**Data sources:**
- Software Composition Analysis (SCA) tools (e.g. Snyk, Grype, Trivy, Black Duck) scanning application dependencies
- Static Application Security Testing (SAST) tools for first-party code vulnerabilities
- National Vulnerability Database (NVD) or OSV for CVSS base scores

**Calculation:**
```
vuln-cvss-max-score = MAX(CVSS_base_score) over all unresolved vulnerabilities in scope
```

1. Collect all open, unresolved vulnerabilities for the product from the scanning toolchain.
2. Filter to vulnerabilities that have a valid CVSS v3.x base score assigned.
3. Return the single highest score across that set.

**Automation:** Can be extracted directly from most SCA/SAST tool APIs or SARIF output. The NVD API can be used to enrich findings with CVSS scores where the tool does not provide them.

# Notes

- **Base score only:** Use the CVSS *base* score for consistency. Temporal and environmental scores may be more accurate for your context but vary by organisation and are harder to automate reliably.
- **Scope:** Define clearly whether "in scope" includes all dependencies (transitive included), first-party code findings, container base image vulnerabilities, or a subset. Consistency is critical for trend tracking.
- **False positives:** Accepted/suppressed vulnerabilities (with documented justification) should be excluded from the calculation to avoid metric noise.
- **Versioning:** If CVSS v4.0 scores become available for a vulnerability, prefer v4.0, but do not mix versions across findings in the same calculation.
