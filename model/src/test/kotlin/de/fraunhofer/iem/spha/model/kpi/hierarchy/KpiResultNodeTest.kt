/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi.hierarchy

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KpiResultNodeTest {
    @Test
    fun testDefaultConstructor() {
        // When using the default constructor, a random UUID should be generated
        val node1 =
            KpiResultNode(
                typeId = "test",
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = emptyList(),
                thresholds = mapOf(),
            )
        val node2 =
            KpiResultNode(
                typeId = "test",
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = emptyList(),
                thresholds = mapOf(),
            )

        // Verify that the IDs are different
        assertNotEquals(node1.id, node2.id)

        // Verify other properties
        assertEquals("test", node1.typeId)
        assertEquals(KpiCalculationResult.Success(100), node1.result)
        assertEquals(KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY, node1.strategy)
        assertEquals(emptyList(), node1.edges)
        assertEquals(emptyMap(), node1.thresholds)
        assertEquals(null, node1.originId)
        assertEquals(null, node1.reason)
    }

    @Test
    fun testCustomIdConstructor() {
        // When using the constructor with a custom ID, that ID should be used
        val customId = "custom-id-123"
        val node =
            KpiResultNode(
                typeId = "test",
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = emptyList(),
                id = customId,
                originId = "origin-123",
                reason = "test reason",
                thresholds = mapOf("warning" to 50, "critical" to 90),
            )

        // Verify that the custom ID is used
        assertEquals(customId, node.id)

        // Verify other properties
        assertEquals("test", node.typeId)
        assertEquals(KpiCalculationResult.Success(100), node.result)
        assertEquals(KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY, node.strategy)
        assertEquals(emptyList(), node.edges)
        assertEquals("origin-123", node.originId)
        assertEquals("test reason", node.reason)
        assertEquals(mapOf("warning" to 50, "critical" to 90), node.thresholds)
    }

    @Test
    fun testEquality() {
        // Two nodes with the same properties but different IDs should be equal
        // because ID is not part of equals/hashCode
        val node1 =
            KpiResultNode(
                typeId = "test",
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = emptyList(),
                originId = "origin-123",
                reason = "test reason",
            )
        val node2 =
            KpiResultNode(
                typeId = "test",
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = emptyList(),
                originId = "origin-123",
                reason = "test reason",
            )

        // IDs should be different
        assertNotEquals(node1.id, node2.id)

        // But the objects should be equal
        assertEquals(node1, node2)
        assertEquals(node1.hashCode(), node2.hashCode())
    }

    @Test
    fun testCustomIdEquality() {
        // Two nodes with the same properties and custom IDs should still be equal
        // because ID is not part of equals/hashCode
        val node1 =
            KpiResultNode(
                typeId = "test",
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = emptyList(),
                id = "same-id",
                originId = "origin-123",
                reason = "test reason",
                thresholds = mapOf("warning" to 50, "critical" to 90),
            )
        val node2 =
            KpiResultNode(
                typeId = "test",
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = emptyList(),
                id = "same-id",
                originId = "origin-123",
                reason = "test reason",
                thresholds = mapOf("warning" to 50, "critical" to 90),
            )

        // IDs should be the same
        assertEquals(node1.id, node2.id)

        // And the objects should be equal
        assertEquals(node1, node2)
        assertEquals(node1.hashCode(), node2.hashCode())
    }
}
