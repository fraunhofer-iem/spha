---
id: dep-maintenance-health
title: Dependency Maintenance Health
phase: build
tags:
  - dependency-management
  - sbom
  - maintenance
  - chaoss
related_tools:
  - GrimoireLab
  - Augur
  - GitHub API
  - GitLab API
  - Libraries.io
  - deps.dev
depends_on: []
thresholds:
  - name: Healthy
    value: ">= 0.70"
    description: Project shows consistent release activity, recent commits, and is not critically dependent on a single contributor.
  - name: At Risk
    value: "0.40 – 0.69"
    description: One or more maintenance signals are degraded. Monitor for further decline and consider alternatives.
  - name: Unhealthy
    value: "< 0.40"
    description: Project shows signs of abandonment, single-maintainer dependency, or severely irregular releases. Treat as high operational risk.
references:
  - https://chaoss.community/kb/metric-release-frequency/
  - https://chaoss.community/kb/metric-contributor-absence-factor/
  - https://chaoss.community/practitioner-guide-viability/
  - https://chaoss.community/?p=3944
  - https://deps.dev/
---

# Description

Measures the active maintenance health of an open source dependency by combining three independently automatable signals drawn from the CHAOSS project's viability and community health metrics framework. Together these signals answer the key question: *can we rely on this dependency continuing to receive security fixes, bug fixes, and active development?*

The three signals are:

- **Release Frequency** *(CHAOSS metric)* — how regularly the project ships releases, including patch releases that carry security fixes
- **Contributor Absence Factor** *(CHAOSS metric)* — the smallest number of contributors responsible for 50% of commits; a low value indicates a project is at risk of stalling if a key contributor leaves (the XZ and OpenSSL incidents are canonical examples of this risk)
- **Days Since Last Commit** — the recency of any commit activity to the main branch, as a direct abandonment signal

The result is a normalised score (0.0–1.0) where **higher is healthier**. This score is inverted before inclusion in `dep-risk-score`, so that an unhealthy dependency increases risk.

# Measurement

**Data sources:**
- Repository host API (GitHub API, GitLab API) — commit history, contributor statistics, release/tag history
- [deps.dev](https://deps.dev/) (Google Open Source Insights) — provides package-level repository links, release history, and contributor data for many ecosystems without requiring direct GitHub API calls
- Libraries.io API — alternative source for release history and repository activity across ecosystems

---

## Signal 1: Release Frequency Score

**Definition (CHAOSS):** The number of releases (including point releases) published in the trailing 12 months.

```
releases_per_year(d) = COUNT(releases published in last 365 days for package d)

norm_release_freq(d) = MIN(releases_per_year(d) / 12.0, 1.0)
```

A project releasing monthly or more frequently scores 1.0. The cap of 12 reflects a monthly cadence as the healthy baseline; this can be adjusted for ecosystems with different release norms (e.g. some stable C libraries release annually by design).

---

## Signal 2: Contributor Absence Factor Score

**Definition (CHAOSS):** The smallest number of contributors whose combined commits account for 50% of all commits over the trailing 12 months. A factor of 1 means a single person is responsible for the majority of work. Higher is more resilient.

```
caf(d) = smallest N such that top-N contributors account for >= 50% of commits (trailing 12 months)

norm_caf(d) = MIN((caf(d) - 1) / 4.0, 1.0)
```

Normalisation maps the range [1, 5+] to [0.0, 1.0]:
- CAF = 1 (single maintainer) → score 0.0
- CAF = 2 → score 0.25
- CAF = 3 → score 0.50
- CAF = 5+ → score 1.0

---

## Signal 3: Days Since Last Commit Score

**Definition:** Number of calendar days since the most recent commit to the project's primary branch.

```
days_since_commit(d) = (TODAY - date_of_last_commit(d))

# Saturates at 730 days (2 years); anything beyond that is treated as fully abandoned
norm_last_commit(d) = 1.0 - MIN(days_since_commit(d) / 730.0, 1.0)
```

A commit today scores 1.0; a project with no commit in 2+ years scores 0.0.

---

## Composite Score

```
dep-maintenance-health(d) = norm_release_freq(d) × 0.35
                           + norm_caf(d)          × 0.40
                           + norm_last_commit(d)  × 0.25
```

**Weight rationale:**

| Signal | Weight | Rationale |
|---|---|---|
| Release Frequency | 35% | Security patches reach consumers only via releases; irregular releases are a direct security risk |
| Contributor Absence Factor | 40% | Single-maintainer dependencies are the highest structural risk; the XZ/OpenSSL incidents show this is not theoretical |
| Days Since Last Commit | 25% | A strong abandonment signal, but partially redundant with release frequency for active projects |

**Automation:** All three signals can be retrieved from the GitHub/GitLab API or from deps.dev without authentication for public repositories. For private or hosted mirrors, repository API access is required.

# Notes

- **Score direction:** `dep-maintenance-health` is a *health* score — higher is better. It is **inverted** (`1.0 - score`) when used as a risk input in `dep-risk-score`, so that a healthy dependency does not inflate risk.
- **Stable-by-design packages:** Some packages are intentionally infrequently released because they are feature-complete (e.g. many cryptographic libraries). Low release frequency alone should not be penalised heavily for these. If a package has very low release frequency but recent commits and a good CAF, the composite score will still be reasonable. Annotate known "stable-by-design" packages in the dependency inventory.
- **Monorepos:** Some packages are released from monorepos where commit activity spans many packages. The Days Since Last Commit signal should be scoped to the relevant subdirectory or tag pattern where possible, not the entire monorepo.
- **Non-GitHub/GitLab projects:** Projects hosted on Bitbucket, Sourceforge, or self-hosted forges may not be queryable via the APIs above. deps.dev covers many of these cases; otherwise, manual assessment may be required. Flag such dependencies as having incomplete data.
- **Contributor Absence Factor and bots:** Automated bot commits (e.g. dependency update bots, CI bots) should be excluded from the CAF calculation to avoid inflating the score artificially. CHAOSS explicitly notes this caveat.
- **Archived repositories:** A repository explicitly marked as archived on GitHub/GitLab should be treated as `dep-maintenance-health = 0.0` regardless of other signals, as the project has been officially abandoned by its maintainers.
