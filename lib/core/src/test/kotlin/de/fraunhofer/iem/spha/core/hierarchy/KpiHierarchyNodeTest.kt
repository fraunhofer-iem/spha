/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.hierarchy

import de.fraunhofer.iem.spha.core.randomKpiHierarchyNode
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import kotlin.test.Test
import kotlin.test.assertEquals

class KpiHierarchyNodeTest {
    @Test
    fun testCustomIdConstructor() {
        // Create a KpiNode with a child node
        val childTypeId = "child-test"
        val rootNode =
            KpiNode(
                typeId = "root-test",
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiEdge(
                            target =
                                KpiNode(
                                    typeId = childTypeId,
                                    strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                                    edges = listOf(),
                                ),
                            weight = 1.0,
                        )
                    ),
            )

        // Create a RawValueKpi with the same typeId as the child node and a custom ID
        val customId = "custom-id-123"
        val rawValueKpi = RawValueKpi(typeId = childTypeId, score = 100, id = customId)

        // Create a KpiHierarchyNode using the factory method
        val hierarchyNode = KpiHierarchyNode.from(rootNode, listOf(rawValueKpi))

        // Get the child node
        val childNode = hierarchyNode.edges.first().to

        // Verify that the child node has the custom ID from the RawValueKpi
        assertEquals(customId, childNode.id)

        // Verify that the child node has the correct typeId
        assertEquals(childTypeId, childNode.typeId)

        // Verify that the child node has the correct score
        assertEquals(100, childNode.score)
    }

    @Test
    fun testResultProperty() {
        // Create a KpiHierarchyNode using the factory method
        val node = randomKpiHierarchyNode()

        // Default result should be Empty
        assertEquals(true, node.hasNoResult())

        // Set a new result
        val successResult = KpiCalculationResult.Success(100)
        node.result = successResult

        // Verify the result was set
        assertEquals(successResult, node.result)

        // Verify the score property
        assertEquals(100, node.score)
    }

    @Test
    fun testScoreProperty() {
        // Test the score property with different result types

        // Success result
        val successNode = randomKpiHierarchyNode()
        successNode.result = KpiCalculationResult.Success(100)
        assertEquals(100, successNode.score)

        // Incomplete result
        val incompleteNode = randomKpiHierarchyNode()
        incompleteNode.result = KpiCalculationResult.Incomplete(50, "Incomplete reason")
        assertEquals(50, incompleteNode.score)

        // Empty result
        val emptyNode = randomKpiHierarchyNode()
        assertEquals(0, emptyNode.score)

        // Error result
        val errorNode = randomKpiHierarchyNode()
        errorNode.result = KpiCalculationResult.Error("Error reason")
        assertEquals(0, errorNode.score)
    }

    @Test
    fun testHasNoResult() {
        // Test the hasNoResult method

        // Empty result
        val emptyNode = randomKpiHierarchyNode()
        assertEquals(true, emptyNode.hasNoResult())

        // Error result
        val errorNode = randomKpiHierarchyNode()
        errorNode.result = KpiCalculationResult.Error("Error reason")
        assertEquals(true, errorNode.hasNoResult())

        // Success result
        val successNode = randomKpiHierarchyNode()
        successNode.result = KpiCalculationResult.Success(100)
        assertEquals(false, successNode.hasNoResult())

        // Incomplete result
        val incompleteNode = randomKpiHierarchyNode()
        incompleteNode.result = KpiCalculationResult.Incomplete(50, "Incomplete reason")
        assertEquals(false, incompleteNode.hasNoResult())
    }

    @Test
    fun testHasIncompleteResult() {
        // Test the hasIncompleteResult method

        // Incomplete result
        val incompleteNode = randomKpiHierarchyNode()
        incompleteNode.result = KpiCalculationResult.Incomplete(50, "Incomplete reason")
        assertEquals(true, incompleteNode.hasIncompleteResult())

        // Empty result
        val emptyNode = randomKpiHierarchyNode()
        assertEquals(false, emptyNode.hasIncompleteResult())

        // Error result
        val errorNode = randomKpiHierarchyNode()
        errorNode.result = KpiCalculationResult.Error("Error reason")
        assertEquals(false, errorNode.hasIncompleteResult())

        // Success result
        val successNode = randomKpiHierarchyNode()
        successNode.result = KpiCalculationResult.Success(100)
        assertEquals(false, successNode.hasIncompleteResult())
    }
}
