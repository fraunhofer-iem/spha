/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi.hierarchy

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType

object DefaultHierarchy {
    fun get(): KpiHierarchy {

        val secrets =
            KpiNode(
                kpiId = KpiType.SECRETS.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val documentationInfrastructure =
            KpiNode(
                kpiId = KpiType.DOCUMENTATION_INFRASTRUCTURE.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val commentsInCode =
            KpiNode(
                kpiId = KpiType.COMMENTS_IN_CODE.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val numberOfCommits =
            KpiNode(
                kpiId = KpiType.NUMBER_OF_COMMITS.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val numberOfSignedCommits =
            KpiNode(
                kpiId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val isDefaultBranchProtected =
            KpiNode(
                kpiId = KpiType.IS_DEFAULT_BRANCH_PROTECTED.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val checkedInBinaries =
            KpiNode(
                kpiId = KpiType.CHECKED_IN_BINARIES.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val signedCommitsRatio =
            KpiNode(
                kpiId = KpiType.SIGNED_COMMITS_RATIO.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = numberOfCommits, weight = 1.0),
                        KpiEdge(target = numberOfSignedCommits, weight = 1.0),
                    ),
            )

        val documentation =
            KpiNode(
                kpiId = KpiType.DOCUMENTATION.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = documentationInfrastructure, weight = 0.6),
                        KpiEdge(target = commentsInCode, weight = 0.4),
                    ),
            )

        val processComplianceKpi =
            KpiNode(
                kpiId = KpiType.PROCESS_COMPLIANCE.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = checkedInBinaries, weight = 0.2),
                        KpiEdge(target = signedCommitsRatio, weight = 0.2),
                        KpiEdge(target = isDefaultBranchProtected, weight = 0.3),
                        KpiEdge(target = documentation, weight = 0.3),
                    ),
            )

        val processTransparency =
            KpiNode(
                kpiId = KpiType.PROCESS_TRANSPARENCY.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = signedCommitsRatio, weight = 1.0)),
            )

        val codeVulnerabilities =
            KpiNode(
                kpiId = KpiType.CODE_VULNERABILITY_SCORE.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val maxDepVulnerability =
            KpiNode(
                kpiId = KpiType.MAXIMAL_VULNERABILITY.name,
                kpiStrategyId = KpiStrategyId.MINIMUM_STRATEGY,
                edges = listOf(KpiEdge(target = codeVulnerabilities, weight = 1.0)),
            )

        val containerVulnerabilities =
            KpiNode(
                kpiId = KpiType.CONTAINER_VULNERABILITY_SCORE.name,
                kpiStrategyId = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val maxContainerVulnerability =
            KpiNode(
                kpiId = KpiType.MAXIMAL_VULNERABILITY.name,
                kpiStrategyId = KpiStrategyId.MAXIMUM_STRATEGY,
                edges = listOf(KpiEdge(target = containerVulnerabilities, weight = 1.0)),
            )

        val security =
            KpiNode(
                kpiId = KpiType.SECURITY.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = secrets, weight = 0.2),
                        KpiEdge(target = maxDepVulnerability, weight = 0.35),
                        KpiEdge(target = maxContainerVulnerability, weight = 0.35),
                        KpiEdge(target = checkedInBinaries, weight = 0.1),
                    ),
            )

        val internalQuality =
            KpiNode(
                kpiId = KpiType.INTERNAL_QUALITY.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = documentation, weight = 1.0)),
            )

        val externalQuality =
            KpiNode(
                kpiId = KpiType.EXTERNAL_QUALITY.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = documentation, weight = 1.0)),
            )

        val root =
            KpiNode(
                kpiId = KpiType.ROOT.name,
                kpiStrategyId = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = processTransparency, weight = 0.1),
                        KpiEdge(target = processComplianceKpi, weight = 0.1),
                        KpiEdge(target = security, weight = 0.4),
                        KpiEdge(target = internalQuality, weight = 0.15),
                        KpiEdge(target = externalQuality, weight = 0.25),
                    ),
            )

        return KpiHierarchy.create(root)
    }
}
