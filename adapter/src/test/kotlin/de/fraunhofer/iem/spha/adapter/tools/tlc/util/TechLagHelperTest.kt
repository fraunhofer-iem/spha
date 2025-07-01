/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc.util

import de.fraunhofer.iem.spha.adapter.tools.tlc.TechLagResult
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.Artifact
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.ArtifactVersion
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.Graph
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.Version
import de.fraunhofer.iem.spha.model.adapter.DependencyEdge
import de.fraunhofer.iem.spha.model.adapter.DependencyGraphDto
import de.fraunhofer.iem.spha.model.adapter.DependencyNodeDto
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.junit.jupiter.api.Test

class TechLagHelperTest {

    private fun generateArtifacts(count: Int, versions: List<ArtifactVersion>): List<Artifact> {
        return (0..count).map {
            Artifact(artifactId = "artifact Id $it", groupId = "", versions = versions)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun generateVersions(count: Int): List<ArtifactVersion> {

        var startDate = Clock.System.now()
        val systemTZ = TimeZone.currentSystemDefault()

        return (0..count).mapNotNull {
            val version =
                ArtifactVersion.create(
                    versionNumber = "1.0.$it",
                    releaseDate = startDate.toEpochMilliseconds(),
                    isDefault = false,
                )
            startDate = startDate.plus(1, DateTimeUnit.DAY, systemTZ)
            version
        }
    }

    private fun generateDependencyNodes(count: Int): List<DependencyNodeDto> {
        return (0..count).map { DependencyNodeDto(artifactIdx = it, usedVersion = "1.0.$it") }
    }

    @Test
    fun getTechLagForGraph() {
        val versions = generateVersions(4)
        val artifacts = generateArtifacts(6, versions)
        val nodes = generateDependencyNodes(6)

        val dependencyGraphDto =
            DependencyGraphDto(
                nodes = nodes,
                edges =
                    listOf(
                        DependencyEdge(from = 0, to = 1),
                        DependencyEdge(from = 2, to = 4),
                        DependencyEdge(from = 2, to = 0),
                        DependencyEdge(from = 4, to = 0),
                        DependencyEdge(from = 1, to = 2),
                    ),
                directDependencyIndices = listOf(0, 3),
            )
        // node[0] + 4 days,
        // node[0] + 4 days, (cycle)
        // node[0] + 4 days, (cycle)
        // node[1] + 3 days,
        // node[2] + 2 days,
        // node[3] + 1 day,
        // node[4] + 0 days

        // cyclic graph
        val graph = Graph.from(dependencyGraphDto)

        val techLagResult =
            TechLagHelper.getTechLagForGraph(
                graph = graph,
                artifacts = artifacts,
                targetVersion = Version.Major,
            )

        val isSuccess = techLagResult is TechLagResult.Success
        assert(isSuccess)

        val libyear = (techLagResult as TechLagResult.Success).libyear
        assertEquals(18, libyear)
    }
}
