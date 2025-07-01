/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc.model

import de.fraunhofer.iem.spha.model.adapter.DependencyEdge
import de.fraunhofer.iem.spha.model.adapter.DependencyGraphDto
import de.fraunhofer.iem.spha.model.adapter.DependencyNodeDto
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

class GraphTest {

    private fun getEmptyNodes(count: Int): List<DependencyNodeDto> {
        return (0..count).map { DependencyNodeDto(artifactIdx = it, usedVersion = "1.0.0") }
    }

    @Test
    fun emptyGraph() {
        val dependencyGraphDto = DependencyGraphDto()
        assertDoesNotThrow {
            val graph = Graph.from(dependencyGraphDto = dependencyGraphDto)
            assertEquals(0, graph.size())
        }
    }

    @Test
    fun directDepsOnly() {
        val dependencyGraphDto =
            DependencyGraphDto(
                nodes = getEmptyNodes(4),
                edges = listOf(),
                directDependencyIndices = listOf(0, 3),
            )

        val graph = Graph.from(dependencyGraphDto)

        assertEquals(2, graph.directDependencies.size)
        assertEquals(0, graph.directDependencies[0].children.size)
        assertEquals(0, graph.directDependencies[1].children.size)
        assertEquals(2, graph.size())
    }

    @Test
    fun simpleGraph() {
        val dependencyGraphDto =
            DependencyGraphDto(
                nodes = getEmptyNodes(4),
                edges = listOf(DependencyEdge(from = 0, to = 1), DependencyEdge(from = 1, to = 2)),
                directDependencyIndices = listOf(0, 3),
            )

        val graph = Graph.from(dependencyGraphDto)

        assertEquals(2, graph.directDependencies.size)
        assertEquals(1, graph.directDependencies[0].children.size)
        assertEquals(2, graph.directDependencies[0].size())
        assertEquals(0, graph.directDependencies[1].children.size)
        assertEquals(4, graph.size())
    }

    @Test
    fun multipleChildren() {
        val dependencyGraphDto =
            DependencyGraphDto(
                nodes = getEmptyNodes(4),
                edges =
                    listOf(
                        DependencyEdge(from = 0, to = 1),
                        DependencyEdge(from = 0, to = 4),
                        DependencyEdge(from = 1, to = 2),
                    ),
                directDependencyIndices = listOf(0, 3),
            )

        val graph = Graph.from(dependencyGraphDto)

        assertEquals(2, graph.directDependencies.size)
        assertEquals(2, graph.directDependencies[0].children.size)
        assertEquals(3, graph.directDependencies[0].size())
        assertEquals(0, graph.directDependencies[1].children.size)
        assertEquals(5, graph.size())
    }

    @Test
    fun graphWithCircle() {
        val dependencyGraphDto =
            DependencyGraphDto(
                nodes = getEmptyNodes(4),
                edges =
                    listOf(
                        DependencyEdge(from = 0, to = 1),
                        DependencyEdge(from = 0, to = 4),
                        DependencyEdge(from = 4, to = 0),
                        DependencyEdge(from = 1, to = 2),
                    ),
                directDependencyIndices = listOf(0, 3),
            )

        val graph = Graph.from(dependencyGraphDto)

        assertEquals(2, graph.directDependencies.size)
        assertEquals(2, graph.directDependencies[0].children.size)
        assertEquals(4, graph.directDependencies[0].size())
        assertEquals(0, graph.directDependencies[1].children.size)
        assertEquals(6, graph.size())
    }

    @Test
    fun graphWithMultipleCircles() {
        val dependencyGraphDto =
            DependencyGraphDto(
                nodes = getEmptyNodes(4),
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

        val graph = Graph.from(dependencyGraphDto)

        assertEquals(2, graph.directDependencies.size) // 0, 3
        assertEquals(1, graph.directDependencies[0].children.size) // 0 -> 1
        assertEquals(
            5,
            graph.directDependencies[0].size(),
        ) // 0 -> 1, 1 -> 2, 2 -> 4, 4 -> 0, 2 -> 0
        assertEquals(0, graph.directDependencies[1].children.size)
        assertEquals(7, graph.size())
    }
}
