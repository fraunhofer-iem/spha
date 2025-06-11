/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
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
    LIB_DAYS_DEV,
    LIB_DAYS_PROD,

    // Calculated KPIs
    SIGNED_COMMITS_RATIO,
    INTERNAL_QUALITY,
    EXTERNAL_QUALITY,
    PROCESS_COMPLIANCE,
    PROCESS_TRANSPARENCY,
    SECURITY,
    MAXIMAL_VULNERABILITY,
    DOCUMENTATION,

    // ROOT
    ROOT,
}
