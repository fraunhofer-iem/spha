---
id: dep-vuln-active
title: Dependency Active Vulnerability Count
phase: build
tags:
  - dependency-management
  - sbom
  - vulnerability-management
related_tools:
  - Grype
  - Trivy
  - Snyk
  - Dependabot
  - OSV Scanner
depends_on: []
thresholds:
  - name: Elevated
    value: ">= 1 High or Critical CVE"
    description: At least one unresolved High (CVSS >= 7.0) or Critical (CVSS >= 9.0) vulnerability affects the pinned version.
  - name: High
    value: ">= 3 any-severity CVEs"
    description: Three or more unresolved vulnerabilities of any severity affect the pinned version.
  - name: Target
    value: "0"
    description: No known unresolved vulnerabilities affect the pinned version of this dependency.
references:
  - https://osv.dev/
  - https://nvd.nist.gov/
  - https://github.com/google/osv-scanner
---

# Description

Measures the count of currently known, unresolved vulnerabilities that affect the specific version of a dependency as declared in the product's SBOM. Unlike `dep-vuln-rate`, which captures historical vulnerability frequency at the package level, this metric is scoped to the *exact pinned version* and reflects the *current, unpatched exposure* of that dependency.

This is the most direct signal of immediate risk from a dependency: a non-zero count means the product is knowingly shipping with a vulnerable version. It is used as a sub-metric to compute the dependency risk score (`dep-risk-score`).

# Measurement

**Data sources:**
- SBOM (CycloneDX or SPDX) — provides package name, ecosystem, and pinned version
- OSV.dev API or OSV Scanner — version-aware vulnerability matching
- Grype / Trivy — local SCA scanners that match SBOM components to vulnerability databases by version range

**Calculation:**
```
dep-vuln-active(d) = COUNT(distinct vulnerabilities where
                           affected_version_range contains used_version(d)
                           AND status != resolved/accepted)
```

1. From the SBOM, extract the package name, ecosystem, and pinned version for dependency `d`.
2. Query OSV.dev or run a local SCA scanner against the SBOM to retrieve all vulnerabilities whose affected version range includes the pinned version.
3. Exclude vulnerabilities that have been formally accepted (with documented justification) or marked as not applicable via VEX (Vulnerability Exploitability eXchange) assertions.
4. Return the count of remaining open vulnerabilities.

**Severity breakdown (recommended output):**

In addition to the raw count, emit a severity-stratified breakdown for use in downstream metrics:
```
dep-vuln-active(d) = {
  critical: COUNT(CVSS >= 9.0),
  high:     COUNT(7.0 <= CVSS < 9.0),
  medium:   COUNT(4.0 <= CVSS < 7.0),
  low:      COUNT(CVSS < 4.0),
  total:    COUNT(all)
}
```

**Automation:** OSV Scanner (`osv-scanner`) accepts a CycloneDX or SPDX SBOM as input and produces machine-readable JSON output of matched vulnerabilities per component. This integrates cleanly into CI/CD pipelines.

# Notes

- **Version range accuracy:** Vulnerability databases define affected versions as ranges (e.g. `>= 1.2.0, < 1.2.8`). Ensure the scanner correctly resolves whether the pinned version falls within the range, especially for pre-release or epoch-versioned packages.
- **VEX integration:** If the organisation produces VEX documents (e.g. via OpenVEX or CycloneDX VEX), use them to suppress non-applicable findings. Do not suppress silently — all suppressions should be auditable.
- **Transitive dependencies:** This metric is defined per dependency instance. When applied to transitive dependencies, the same vulnerability may appear in multiple packages in the dependency tree. Each instance should be counted independently at the dependency level; deduplication happens at the product level in `product-operational-risk`.
- **Fixed-but-not-upgraded:** If a vulnerability has a fixed version available but the product has not yet upgraded, the vulnerability must remain counted as active until the upgrade is applied and verified.
