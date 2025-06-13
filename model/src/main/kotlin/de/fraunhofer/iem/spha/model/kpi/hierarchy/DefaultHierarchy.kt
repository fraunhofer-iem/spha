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
                typeId = KpiType.SECRETS.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val documentationInfrastructure =
            KpiNode(
                typeId = KpiType.DOCUMENTATION_INFRASTRUCTURE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val commentsInCode =
            KpiNode(
                typeId = KpiType.COMMENTS_IN_CODE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val numberOfCommits =
            KpiNode(
                typeId = KpiType.NUMBER_OF_COMMITS.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val numberOfSignedCommits =
            KpiNode(
                typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val isDefaultBranchProtected =
            KpiNode(
                typeId = KpiType.IS_DEFAULT_BRANCH_PROTECTED.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val checkedInBinaries =
            KpiNode(
                typeId = KpiType.CHECKED_IN_BINARIES.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val signedCommitsRatio =
            KpiNode(
                typeId = KpiType.SIGNED_COMMITS_RATIO.name,
                strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = numberOfCommits, weight = 1.0),
                        KpiEdge(target = numberOfSignedCommits, weight = 1.0),
                    ),
            )

        val documentation =
            KpiNode(
                typeId = KpiType.DOCUMENTATION.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = documentationInfrastructure, weight = 0.6),
                        KpiEdge(target = commentsInCode, weight = 0.4),
                    ),
            )

        val processComplianceKpi =
            KpiNode(
                typeId = KpiType.PROCESS_COMPLIANCE.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
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
                typeId = KpiType.PROCESS_TRANSPARENCY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = signedCommitsRatio, weight = 1.0)),
            )

        val codeVulnerabilities =
            KpiNode(
                typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val maxDepVulnerability =
            KpiNode(
                typeId = KpiType.MAXIMAL_VULNERABILITY.name,
                strategy = KpiStrategyId.MINIMUM_STRATEGY,
                edges = listOf(KpiEdge(target = codeVulnerabilities, weight = 1.0)),
            )

        val containerVulnerabilities =
            KpiNode(
                typeId = KpiType.CONTAINER_VULNERABILITY_SCORE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )

        val maxContainerVulnerability =
            KpiNode(
                typeId = KpiType.MAXIMAL_VULNERABILITY.name,
                strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                edges = listOf(KpiEdge(target = containerVulnerabilities, weight = 1.0)),
            )

        val security =
            KpiNode(
                typeId = KpiType.SECURITY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
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
                typeId = KpiType.INTERNAL_QUALITY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = documentation, weight = 1.0)),
            )

        val externalQuality =
            KpiNode(
                typeId = KpiType.EXTERNAL_QUALITY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = documentation, weight = 1.0)),
            )

        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
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
