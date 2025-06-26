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
