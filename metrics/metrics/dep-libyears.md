---
id: dep-libyears
title: Dependency Libyears
phase: build
tags:
  - dependency-management
  - sbom
  - technical-debt
related_tools:
  - libyear-bundler
  - libyear (npm/pip/cargo/maven)
  - Renovate
  - Dependabot
  - Snyk
depends_on: []
thresholds:
  - name: Outdated
    value: ">= 1.0 years"
    description: Dependency is at least one major release cycle behind. Upgrade should be planned.
  - name: Critically Outdated
    value: ">= 3.0 years"
    description: Dependency is severely behind. Likely missing security patches and no longer maintained on the used version.
  - name: Target
    value: "< 0.5 years"
    description: Dependency is within half a year of the latest stable release.
references:
  - https://libyear.com/
  - https://arxiv.org/abs/1901.09669
---

# Description

Measures how outdated a single dependency version is, expressed in fractional years between the release date of the version currently in use and the release date of the latest stable version of that package. This concept is known as _libyears_ — one libyear equals one year of accumulated lag for a single dependency.

A high libyear value indicates that a dependency has not been updated in a long time relative to the pace of its upstream development. This is a proxy for accumulated technical debt, exposure to unpatched historical vulnerabilities, and the risk of future incompatibility or abandonment. It is used as a sub-metric to compute the dependency risk score (`dep-risk-score`).

# Measurement

**Data sources:**

- SBOM (CycloneDX or SPDX format) for the product — provides the dependency name and currently used version
- Package registry APIs (e.g. npm registry, PyPI, Maven Central, crates.io, RubyGems) — provide release date of the current version and the latest stable version

**Calculation:**

```
dep-libyears(d) = (release_date(latest_stable_version(d)) - release_date(used_version(d))) / 365.25
```

1. From the SBOM, extract the package name and pinned version for dependency `d`.
2. Query the relevant package registry API to retrieve:
   - The release timestamp of the pinned version
   - The release timestamp of the latest stable (non-prerelease) version
3. Compute the difference in days and divide by 365.25 to express as fractional years.

**Automation:** Several language-specific CLI tools (e.g. `libyear` for npm, pip, cargo, maven) implement this calculation natively. For language-agnostic automation from a CycloneDX SBOM, registry APIs can be queried programmatically per package ecosystem.

# Notes

- **Latest stable only:** Pre-release, alpha, beta, and release candidate versions should be excluded from the "latest" reference point to avoid inflating the score.
- **Actively maintained packages:** Some packages release very infrequently by design (e.g. stable, finished libraries). A high libyear value for such packages does not necessarily indicate neglect — context matters.
- **Yanked or deprecated versions:** If the currently used version has been yanked or deprecated by the maintainer, this should be flagged separately as it may indicate a more urgent upgrade need beyond what the libyear score reflects.
- **Multiple registries:** Products with polyglot dependency trees (e.g. Python + JavaScript + Java) require querying multiple registries. Ensure tooling covers all relevant ecosystems present in the SBOM.
- **Snapshot date:** The libyear value is relative to the date of calculation. Always record the snapshot date alongside the metric value for trend tracking.
