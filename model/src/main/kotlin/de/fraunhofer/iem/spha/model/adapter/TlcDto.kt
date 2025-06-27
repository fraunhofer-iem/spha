/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlinx.serialization.Serializable

@Serializable
data class TlcDto(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val projectDtos: List<ProjectDto>,
) : ToolResult

@Serializable
data class RepositoryInfoDto(
    val url: String,
    val revision: String,
    val projects: List<ProjectInfoDto>,
)

@Serializable
data class ProjectInfoDto(
    val type: String,
    val namespace: String,
    val name: String,
    val version: String,
)

@Serializable data class EnvironmentInfoDto(val ortVersion: String, val javaVersion: String)

@Serializable
data class ProjectDto(
    val artifacts: List<ArtifactDto> = listOf(), // Stores all components and their related metadata
    val graph:
        List<
            ScopeToGraph
        >, // Maps the graphs' scope to the dependency graph extracted from the project
    val ecosystem:
        String, // Used to identify the appropriate APIs to call for additional information
    val version: String = "",
    val artifactId: String = "",
    val groupId: String = "",
) : Origin

@Serializable
data class ArtifactDto(
    val artifactId: String,
    val groupId: String,
    val versions: List<ArtifactVersionDto> = listOf(),
)

@Serializable
data class ArtifactVersionDto(
    val versionNumber: String,
    val releaseDate: Long,
    val isDefault: Boolean,
)

@Serializable
data class ScopeToVersionToGraph(val scope: String, val versionToGraph: List<VersionToGraph>)

@Serializable data class VersionToGraph(val version: String, val graph: DependencyGraphDto)

@Serializable data class ScopeToGraph(val scope: String, val graph: DependencyGraphDto)

@Serializable
data class DependencyGraphDto(
    val nodes: List<DependencyNodeDto> = listOf(),
    val edges: List<DependencyEdge> = listOf(),
    val directDependencyIndices: List<Int> =
        listOf(), // Idx of the nodes' which are direct dependencies of this graph
)

@Serializable
data class DependencyNodeDto(
    val artifactIdx: Int, // Index of the artifact in the DependencyGraphs' artifacts list
    val usedVersion: String,
)

@Serializable
data class DependencyEdge(
    // Indices of the nodes in the DependencyGraph's nodes list
    val from: Int,
    val to: Int,
)

@Serializable
data class GraphMetadata(
    val numberOfNodes: Int,
    val numberOfEdges: Int,
    val percentageOfNodesWithStats: Double,
)

@Serializable
data class TlcConfig(val thresholds: Collection<RangeThreshold>) {
    init {
        require(validThresholds(thresholds)) { "TlcConfig ranges are invalid" }
    }

    private fun validThresholds(rangeThresholds: Collection<RangeThreshold>): Boolean {

        val sortedThresholds = rangeThresholds.sortedBy { it.range.from }

        // ranges should be mutually exclusive
        sortedThresholds.forEachIndexed { index, threshold ->
            if (index == sortedThresholds.size - 1) {
                return@forEachIndexed
            }

            val next = sortedThresholds[index + 1]
            if (
                next.range.from != threshold.range.to + 1 ||
                    threshold.range.from > threshold.range.to
            ) {
                return false
            }
        }

        return true
    }
}

@Serializable
data class RangeThreshold(val score: Int, val range: Range) {
    init {
        require(score in 0..100) { "Thresholds must be between 0 and 100" }
    }
}

@Serializable
data class Range(
    // inclusive ranges
    // from: 0 to 20 means that 0 and 20 are in the range
    // and the next range must start at 21
    val from: Long,
    val to: Long,
)

object TlcDefaultConfig {
    fun get(): TlcConfig {
        return TlcConfig(
            listOf(
                RangeThreshold(score = 100, Range(from = 0, to = 60)),
                RangeThreshold(score = 75, Range(from = 61, to = 120)),
                RangeThreshold(score = 50, Range(from = 121, to = 180)),
                RangeThreshold(score = 25, Range(from = 181, to = 360)),
            )
        )
    }
}
