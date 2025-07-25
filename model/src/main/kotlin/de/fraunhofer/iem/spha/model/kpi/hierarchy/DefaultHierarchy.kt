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

        val documentationInfrastructure =
            KpiNode(
                typeId = KpiType.DOCUMENTATION_INFRASTRUCTURE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Documentation Infrastructure",
                        description =
                            "Measures the quality and completeness of project documentation infrastructure",
                        tags = setOf("documentation", "infrastructure"),
                    ),
            )

        val commentsInCode =
            KpiNode(
                typeId = KpiType.COMMENTS_IN_CODE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Comments in Code",
                        description =
                            "Measures the quality and quantity of comments in the codebase",
                        tags = setOf("documentation", "code-quality"),
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

        val checkedInBinaries =
            KpiNode(
                typeId = KpiType.CHECKED_IN_BINARIES.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(
                        displayName = "Checked In Binaries",
                        description = "Measures the presence of binary files in the repository",
                        tags = setOf("version-control", "best-practices"),
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

        val documentation =
            KpiNode(
                typeId = KpiType.DOCUMENTATION.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(target = documentationInfrastructure, weight = 0.6),
                        KpiEdge(target = commentsInCode, weight = 0.4),
                    ),
                metaInfo =
                    MetaInfo(
                        displayName = "Documentation",
                        description = "Measures the overall quality of project documentation",
                        tags = setOf("documentation", "quality"),
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
                        KpiEdge(target = secrets, weight = 0.2),
                        KpiEdge(target = maxDepVulnerability, weight = 0.35),
                        KpiEdge(target = maxContainerVulnerability, weight = 0.35),
                        KpiEdge(target = checkedInBinaries, weight = 0.1),
                    ),
                metaInfo =
                    MetaInfo(
                        displayName = "Security",
                        description = "Measures the overall security of the project",
                        tags = setOf("security", "overall"),
                    ),
            )

        val internalQuality =
            KpiNode(
                typeId = KpiType.INTERNAL_QUALITY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = documentation, weight = 1.0)),
                metaInfo =
                    MetaInfo(
                        displayName = "Internal Quality",
                        description = "Measures the internal quality aspects of the project",
                        tags = setOf("quality", "internal"),
                    ),
            )

        val externalQuality =
            KpiNode(
                typeId = KpiType.EXTERNAL_QUALITY.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(KpiEdge(target = documentation, weight = 1.0)),
                metaInfo =
                    MetaInfo(
                        displayName = "External Quality",
                        description = "Measures the external quality aspects of the project",
                        tags = setOf("quality", "external"),
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
                        KpiEdge(target = internalQuality, weight = 0.15),
                        KpiEdge(target = externalQuality, weight = 0.25),
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
}
