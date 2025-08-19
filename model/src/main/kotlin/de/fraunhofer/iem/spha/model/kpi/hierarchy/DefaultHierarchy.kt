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
                metaInfo =
                    MetaInfo(
                        displayName = "Secrets",
                        description =
                            "Measures the presence of secrets or sensitive information in the codebase",
                        tags = setOf("security", "sensitive-data"),
                    ),
            )

        val numberOfCommits =
            KpiNode(
                typeId = KpiType.NUMBER_OF_COMMITS.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Number of Commits",
                        description = "Measures the total number of commits in the repository",
                        tags = setOf("version-control", "activity"),
                    ),
            )

        val numberOfSignedCommits =
            KpiNode(
                typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Number of Signed Commits",
                        description =
                            "Measures the number of commits that are cryptographically signed",
                        tags = setOf("version-control", "security", "authentication"),
                    ),
            )

        val isDefaultBranchProtected =
            KpiNode(
                typeId = KpiType.IS_DEFAULT_BRANCH_PROTECTED.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Is Default Branch Protected?",
                        description =
                            "Indicates whether the default branch has protection rules enabled",
                        tags = setOf("version-control", "security", "branch-protection"),
                    ),
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
                metaInfo =
                    MetaInfo(
                        displayName = "Signed Commits Ratio",
                        description = "Measures the ratio of signed commits to total commits",
                        tags = setOf("version-control", "security", "authentication"),
                    ),
            )

        val processComplianceKpi =
            KpiNode(
                typeId = KpiType.PROCESS_COMPLIANCE.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = signedCommitsRatio, weight = 0.3),
                        KpiEdge(target = isDefaultBranchProtected, weight = 0.7),
                    ),
                metaInfo =
                    MetaInfo(
                        displayName = "Process Compliance",
                        description = "Measures compliance with development process best practices",
                        tags = setOf("process", "compliance", "best-practices"),
                    ),
            )

        val processTransparency =
            KpiNode(
                typeId = KpiType.PROCESS_TRANSPARENCY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = signedCommitsRatio, weight = 1.0)),
                metaInfo =
                    MetaInfo(
                        displayName = "Process Transparency",
                        description = "Measures the transparency of the development process",
                        tags = setOf("process", "transparency"),
                    ),
            )

        val codeVulnerabilities =
            KpiNode(
                typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Code Vulnerabilities",
                        description = "Measures the presence of vulnerabilities in the code",
                        tags = setOf("security", "vulnerabilities", "code"),
                    ),
            )

        val maxDepVulnerability =
            KpiNode(
                typeId = KpiType.MAXIMAL_VULNERABILITY.name,
                strategy = KpiStrategyId.MINIMUM_STRATEGY,
                edges = listOf(KpiEdge(target = codeVulnerabilities, weight = 1.0)),
                metaInfo =
                    MetaInfo(
                        displayName = "Maximal Dependency Vulnerability",
                        description = "Measures the most severe vulnerability in dependencies",
                        tags = setOf("security", "vulnerabilities", "dependencies"),
                    ),
            )

        val containerVulnerabilities =
            KpiNode(
                typeId = KpiType.CONTAINER_VULNERABILITY_SCORE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Container Vulnerabilities",
                        description = "Measures the presence of vulnerabilities in containers",
                        tags = setOf("security", "vulnerabilities", "containers"),
                    ),
            )

        val maxContainerVulnerability =
            KpiNode(
                typeId = KpiType.MAXIMAL_VULNERABILITY.name,
                strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                edges = listOf(KpiEdge(target = containerVulnerabilities, weight = 1.0)),
                metaInfo =
                    MetaInfo(
                        displayName = "Maximal Container Vulnerability",
                        description = "Measures the most severe vulnerability in containers",
                        tags = setOf("security", "vulnerabilities", "containers"),
                    ),
            )

        val security =
            KpiNode(
                typeId = KpiType.SECURITY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = secrets, weight = 0.3),
                        KpiEdge(target = maxDepVulnerability, weight = 0.35),
                        KpiEdge(target = maxContainerVulnerability, weight = 0.35),
                    ),
                metaInfo =
                    MetaInfo(
                        displayName = "Security",
                        description = "Measures the overall security of the project",
                        tags = setOf("security", "overall"),
                    ),
            )

        val quality =
            KpiNode(
                typeId = KpiType.QUALITY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = getTechLag(), weight = 1.0)),
                metaInfo =
                    MetaInfo(
                        displayName = "Quality",
                        description =
                            "Measures the quality and maintainability aspects of the project",
                        tags = setOf("quality"),
                    ),
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
                        KpiEdge(target = quality, weight = 0.15),
                    ),
                metaInfo =
                    MetaInfo(
                        displayName = "Software Product Health Score",
                        description = "Overall health score of the software product",
                        tags = setOf("health", "overall", "root"),
                    ),
            )

        return KpiHierarchy.create(root)
    }

    private fun getTechLag(): KpiNode {

        val techLagProd =
            KpiNode(
                typeId = KpiType.TECHNICAL_LAG_PROD.name,
                KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            KpiNode(
                                typeId = KpiType.TECHNICAL_LAG_PROD_DIRECT.name,
                                KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                                edges =
                                    listOf(
                                        KpiEdge(
                                            KpiNode(
                                                typeId = KpiType.HIGHEST_LIB_DAYS_PROD_DIRECT.name,
                                                KpiStrategyId.RAW_VALUE_STRATEGY,
                                                thresholds = listOf(Threshold("maximum", 60)),
                                                edges = listOf(),
                                            ),
                                            weight = 0.5,
                                        ),
                                        KpiEdge(
                                            KpiNode(
                                                typeId =
                                                    KpiType.TECHNICAL_LAG_PROD_DIRECT_COMPONENT
                                                        .name,
                                                KpiStrategyId.RAW_VALUE_STRATEGY,
                                                thresholds =
                                                    listOf(
                                                        Threshold(
                                                            "maximum",
                                                            30, // days
                                                        )
                                                    ),
                                                edges = listOf(),
                                            ),
                                            weight = 0.5,
                                        ),
                                    ),
                            ),
                            weight = 0.7,
                        ),
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.TECHNICAL_LAG_PROD_TRANSITIVE.name,
                                    KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                KpiNode(
                                                    typeId =
                                                        KpiType.HIGHEST_LIB_DAYS_PROD_TRANSITIVE
                                                            .name,
                                                    KpiStrategyId.RAW_VALUE_STRATEGY,
                                                    thresholds = listOf(Threshold("maximum", 180)),
                                                    edges = listOf(),
                                                ),
                                                weight = 0.5,
                                            ),
                                            KpiEdge(
                                                KpiNode(
                                                    typeId =
                                                        KpiType
                                                            .TECHNICAL_LAG_PROD_TRANSITIVE_COMPONENT
                                                            .name,
                                                    KpiStrategyId.RAW_VALUE_STRATEGY,
                                                    thresholds =
                                                        listOf(
                                                            Threshold(
                                                                "maximum",
                                                                90, // days
                                                            )
                                                        ),
                                                    edges = listOf(),
                                                ),
                                                weight = 0.5,
                                            ),
                                        ),
                                ),
                            weight = 0.3,
                        ),
                    ),
            )

        val techLagDev =
            KpiNode(
                typeId = KpiType.TECHNICAL_LAG_DEV.name,
                KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            KpiNode(
                                typeId = KpiType.TECHNICAL_LAG_DEV_DIRECT.name,
                                KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                                edges =
                                    listOf(
                                        KpiEdge(
                                            KpiNode(
                                                typeId = KpiType.HIGHEST_LIB_DAYS_DEV_DIRECT.name,
                                                KpiStrategyId.RAW_VALUE_STRATEGY,
                                                thresholds = listOf(Threshold("maximum", 60)),
                                                edges = listOf(),
                                            ),
                                            weight = 0.5,
                                        ),
                                        KpiEdge(
                                            KpiNode(
                                                typeId =
                                                    KpiType.TECHNICAL_LAG_DEV_DIRECT_COMPONENT.name,
                                                KpiStrategyId.RAW_VALUE_STRATEGY,
                                                thresholds =
                                                    listOf(
                                                        Threshold(
                                                            "maximum",
                                                            30, // days
                                                        )
                                                    ),
                                                edges = listOf(),
                                            ),
                                            weight = 0.5,
                                        ),
                                    ),
                            ),
                            weight = 0.7,
                        ),
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.TECHNICAL_LAG_DEV_TRANSITIVE.name,
                                    KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                KpiNode(
                                                    typeId =
                                                        KpiType.HIGHEST_LIB_DAYS_DEV_TRANSITIVE
                                                            .name,
                                                    KpiStrategyId.RAW_VALUE_STRATEGY,
                                                    thresholds = listOf(Threshold("maximum", 180)),
                                                    edges = listOf(),
                                                ),
                                                weight = 0.5,
                                            ),
                                            KpiEdge(
                                                KpiNode(
                                                    typeId =
                                                        KpiType
                                                            .TECHNICAL_LAG_DEV_TRANSITIVE_COMPONENT
                                                            .name,
                                                    KpiStrategyId.RAW_VALUE_STRATEGY,
                                                    thresholds =
                                                        listOf(
                                                            Threshold(
                                                                "maximum",
                                                                90, // days
                                                            )
                                                        ),
                                                    edges = listOf(),
                                                ),
                                                weight = 0.5,
                                            ),
                                        ),
                                ),
                            weight = 0.3,
                        ),
                    ),
            )

        val techLag =
            KpiNode(
                typeId = KpiType.TECHNICAL_LAG.name,
                KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = techLagProd, weight = 0.9),
                        KpiEdge(target = techLagDev, weight = 0.1),
                    ),
            )

        return techLag
    }
}
