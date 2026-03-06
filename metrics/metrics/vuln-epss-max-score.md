---
id: vuln-epss-max-score
title: Maximum Vulnerability EPSS Score
phase: operate
tags:
  - vulnerability-management
  - epss
  - exploitability
  - security-posture
related_tools:
  - Snyk
  - Grype
  - Nucleus Security
  - Tenable
depends_on: []
thresholds:
  - name: Critical Exploitability
    value: ">= 0.90"
    description: Over 90% probability of exploitation in the wild within 30 days. Treat as immediate priority regardless of CVSS score.
  - name: High Exploitability
    value: ">= 0.50"
    description: Greater than 50% exploitation probability. Accelerate remediation timeline.
  - name: Target
    value: "< 0.10"
    description: All unresolved vulnerabilities have low exploitation probability.
references:
  - https://www.first.org/epss/
  - https://api.first.org/epss
---

# Description

Measures the highest EPSS (Exploit Prediction Scoring System) score across all known, unresolved vulnerabilities in a software product. EPSS is a data-driven model maintained by FIRST that estimates the probability (0.0–1.0) that a given CVE will be exploited in the wild within the next 30 days, based on threat intelligence, public exploit availability, and historical exploitation patterns.

Unlike CVSS, which measures the *severity* of a vulnerability in isolation, EPSS measures *real-world exploitability likelihood*. A vulnerability with a moderate CVSS score but a high EPSS score may represent a greater immediate risk than a critical CVSS-rated vulnerability that has never been observed being exploited.

This metric is used as a sub-metric to compute the composite Maximum Vulnerability Score (`vuln-max-score`).

# Measurement

**Data sources:**
- FIRST EPSS API (`https://api.first.org/data/v1/epss`) — queryable by CVE ID, updated daily
- SCA tools with native EPSS enrichment (e.g. Grype, Nucleus Security, Tenable)

**Calculation:**
```
vuln-epss-max-score = MAX(EPSS_score) over all unresolved vulnerabilities in scope
```

1. Collect all open, unresolved vulnerabilities with valid CVE identifiers.
2. Query the EPSS API (or use tool-native enrichment) to retrieve the current EPSS score for each CVE.
3. Return the single highest EPSS score across the set.

**Automation:** The FIRST EPSS API is free and publicly accessible. It supports bulk CVE lookups, making it straightforward to enrich an existing vulnerability list as part of a CI/CD pipeline or scheduled scan job.

# Notes

- **EPSS is dynamic:** Scores are updated daily as new threat intelligence is ingested. The same CVE can shift significantly in score over time. Point-in-time snapshots should be timestamped.
- **CVE requirement:** EPSS scores are only available for CVEs with an assigned CVE ID. Vulnerabilities without a CVE (e.g. some proprietary code findings) cannot be scored with EPSS.
- **Not all vulnerabilities have EPSS scores:** Newly published CVEs may not yet have an EPSS score. These should be tracked separately and not excluded silently.
- **Complement to CVSS:** EPSS should never be used as a standalone replacement for CVSS. The two scores measure different dimensions of risk and are most useful in combination (see `vuln-max-score`).
