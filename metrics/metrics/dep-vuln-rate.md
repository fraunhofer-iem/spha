---
id: dep-vuln-rate
title: Dependency Vulnerability Rate
phase: build
tags:
  - dependency-management
  - sbom
  - vulnerability-management
related_tools:
  - OSV.dev
  - Snyk
  - GitHub Advisory Database
  - NVD
depends_on: []
thresholds:
  - name: Elevated
    value: ">= 2 CVEs / year"
    description: Package has had two or more published vulnerabilities in the past 12 months. Treat as a signal of active attack surface.
  - name: High
    value: ">= 5 CVEs / year"
    description: Package is a frequent vulnerability source. Evaluate whether a safer alternative exists.
  - name: Target
    value: "0 CVEs / year"
    description: No vulnerabilities published for this package in the past 12 months.
references:
  - https://osv.dev/
  - https://github.com/advisories
  - https://nvd.nist.gov/
---

# Description

Measures the number of publicly disclosed vulnerabilities (CVEs or equivalent advisory records) published against a specific package within the trailing 12-month window. This captures the *historical vulnerability frequency* of a package — an indicator of how actively it attracts security issues, which informs the likelihood of future vulnerabilities.

A package with a high vulnerability rate may represent a structurally risky dependency regardless of its current patched state: frequent past vulnerabilities suggest the codebase is complex, widely targeted, or under active security scrutiny. This metric is used as a sub-metric to compute the dependency risk score (`dep-risk-score`).

# Measurement

**Data sources:**
- [OSV.dev](https://osv.dev/) API — open, cross-ecosystem vulnerability database covering npm, PyPI, Maven, Go, Rust, and more
- GitHub Advisory Database (GHSA) — community-curated advisories linked to package ecosystems
- NVD (National Vulnerability Database) — for CVEs with CPE mappings to package names

**Calculation:**
```
dep-vuln-rate(d) = COUNT(vulnerabilities published against package d
                         with disclosure_date >= TODAY - 365 days)
```

1. From the SBOM, extract the package name and ecosystem (e.g. `npm`, `pypi`, `maven`).
2. Query OSV.dev or GitHub Advisory Database for all advisories matching the package name and ecosystem.
3. Filter to records with a `published` or `modified` date within the last 365 days.
4. Return the count of distinct vulnerability records.

**Automation:** OSV.dev provides a free REST API and supports batch queries by package name and ecosystem. This makes it straightforward to enrich an SBOM programmatically. The query should target the package name rather than a specific version, as the intent is to measure the package's overall vulnerability history.

# Notes

- **Package-level, not version-level:** This metric counts vulnerabilities against the package as a whole, not only those affecting the specific version in use. A fixed vulnerability still signals that the package has been a vulnerability target.
- **Deduplication:** The same vulnerability may appear across multiple databases (NVD, OSV, GHSA). Deduplicate by CVE ID or OSV ID before counting to avoid inflation.
- **Ecosystem specificity:** Some packages share names across ecosystems (e.g. `requests` in PyPI vs. a hypothetical npm package). Always scope queries by both package name and ecosystem to avoid cross-contamination.
- **Disclosure lag:** Some vulnerabilities are disclosed months or years after discovery. The trailing 12-month window may not capture all vulnerabilities that were *present* in the dependency during that period, only those *disclosed* during it.
- **High-profile packages:** Widely used packages (e.g. OpenSSL, Log4j) naturally attract more vulnerability research and may show elevated rates that partly reflect scrutiny rather than pure risk. This should be noted when interpreting outliers.
