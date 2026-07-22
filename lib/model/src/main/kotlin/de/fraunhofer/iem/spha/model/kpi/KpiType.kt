/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi

enum class KpiType {
    // Raw Value KPIs
    CHECKED_IN_BINARIES,
    NUMBER_OF_COMMITS,
    CODE_VULNERABILITY_SCORE,
    CONTAINER_VULNERABILITY_SCORE,
    NUMBER_OF_SIGNED_COMMITS,
    IS_DEFAULT_BRANCH_PROTECTED,
    SECRETS,
    SAST_USAGE,
    COMMENTS_IN_CODE,
    DOCUMENTATION_INFRASTRUCTURE,
    HIGHEST_LIB_DAYS_DEV_TRANSITIVE,
    HIGHEST_LIB_DAYS_PROD_TRANSITIVE,
    HIGHEST_LIB_DAYS_DEV_DIRECT,
    HIGHEST_LIB_DAYS_PROD_DIRECT,
    TECHNICAL_LAG_DEV_TRANSITIVE_COMPONENT,
    TECHNICAL_LAG_PROD_TRANSITIVE_COMPONENT,
    TECHNICAL_LAG_DEV_DIRECT_COMPONENT,
    TECHNICAL_LAG_PROD_DIRECT_COMPONENT,

    // Quality-gate raw-value KPIs (SecObserve + SBOM; boolean 100=pass / 0=fail).
    // B1: 100 = no non-suppressed known-exploited (KEV) finding, 0 = at least one.
    KNOWN_EXPLOITED_VULNERABILITIES,
    // B2: 100 = no non-suppressed finding at/over the severity threshold, 0 = at least one.
    SEVERITY_THRESHOLD_FINDINGS,
    // B3: 100 = a valid CycloneDX SBOM within the freshness window, 0 = stale/missing/invalid.
    SBOM_FRESHNESS,

    // Calculated KPIs
    QUALITY,
    TECHNICAL_LAG,
    TECHNICAL_LAG_DEV,
    TECHNICAL_LAG_PROD,
    TECHNICAL_LAG_DEV_DIRECT,
    TECHNICAL_LAG_PROD_DIRECT,
    TECHNICAL_LAG_DEV_TRANSITIVE,
    TECHNICAL_LAG_PROD_TRANSITIVE,
    SIGNED_COMMITS_RATIO,
    INTERNAL_QUALITY,
    EXTERNAL_QUALITY,
    PROCESS_COMPLIANCE,
    PROCESS_TRANSPARENCY,
    SECURITY,
    MAXIMAL_VULNERABILITY,
    DOCUMENTATION,

    // Quality-gate aggregate: AND over B1/B2/B3 (100 iff all three leaves are 100).
    BLOCKING_GATE,

    // ROOT
    ROOT,
}
