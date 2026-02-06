/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.transformation

import de.fraunhofer.iem.spha.core.hierarchy.KpiHierarchyNode
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import de.fraunhofer.iem.spha.model.kpi.hierarchy.Threshold
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class RawValueTransformerTest {

    private fun createTestNode(
        typeId: String = KpiType.TECHNICAL_LAG_DEV_DIRECT_COMPONENT.name,
        score: Int = 30,
        thresholds: List<Threshold> = listOf(Threshold("threshold", 50)),
    ): KpiHierarchyNode {
        val kpiNode =
            KpiNode(
                typeId = typeId,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = typeId,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = emptyList(),
                                    thresholds = thresholds,
                                ),
                            weight = 1.0,
                        )
                    ),
                thresholds = thresholds,
            )
        // incorrect setup of test nodes. node will always have a score of 0.
        val rawValueKpi = RawValueKpi(typeId = typeId, score = score)
        val node = KpiHierarchyNode.from(kpiNode, listOf(rawValueKpi))
        return node.edges.first().to
    }

    @Test
    fun testTransformWithTechnicalLagComponent() {
        // Given a node with TECHNICAL_LAG_DEV_DIRECT_COMPONENT type and thresholds
        val node = createTestNode(score = 30, thresholds = listOf(Threshold("threshold", 50)))

        // When transforming the node
        val result = DefaultRawValueTransformer.transform(node)

        // Then it should apply tech lag transformation
        assertTrue(result is KpiCalculationResult.Success)
        assertEquals(100, result.score) // 30 <= 50, so the score should be 100
    }

    @Test
    fun testTransformWithNonTechnicalLagComponent() {
        // Given a node with a non-technical lag type
        val node = createTestNode(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 75)
        val originalResult = node.result

        // When transforming the node
        val result = DefaultRawValueTransformer.transform(node)

        // Then it should return the original result unchanged
        assertEquals(originalResult, result)
    }

    @Test
    fun testTransformTechLagComponentScoreAtThreshold() {
        // Given a node with a score exactly at the threshold
        val node = createTestNode(score = 50, thresholds = listOf(Threshold("threshold", 50)))

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then the score should be 100
        assertTrue(result is KpiCalculationResult.Success)
        assertEquals(100, result.score)
    }

    @Test
    fun testTransformTechLagComponentScoreBetweenThresholdAndDoubleThreshold() {
        // Given a node with a score between the threshold and 2*threshold (75 is between 50 and
        // 100)
        val node = createTestNode(score = 75, thresholds = listOf(Threshold("threshold", 50)))

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then the score should be calculated using linear interpolation
        assertTrue(result is KpiCalculationResult.Success)
        // Since node.score = 75 and threshold = 50:
        // Expected: (1 - ((75 - 50) / 50)) * 100 = (1 - 0.5) * 100 = 50
        assertEquals(50.0, result.score.toDouble(), 0.1)
    }

    @Test
    fun testTransformTechLagComponentScoreAboveDoubleThreshold() {
        // Given a node with score above 2*threshold (150 > 100)
        val node = createTestNode(score = 150, thresholds = listOf(Threshold("threshold", 50)))

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then score should be 0
        assertTrue(result is KpiCalculationResult.Success)
        assertEquals(0, result.score)
    }

    @Test
    fun testTransformTechLagComponentWithEmptyThresholds() {
        // Given a node with no thresholds
        val node = createTestNode(score = 50, thresholds = emptyList())

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then it should return an error
        assertTrue(result is KpiCalculationResult.Error)
        assertTrue(result.reason.contains("Thresholds for node"))
        assertTrue(result.reason.contains("are empty"))
    }

    @Test
    fun testTransformTechLagComponentWithNegativeScore() {
        // Given a node with negative score - need to manually set negative result since RawValueKpi
        // doesn't allow negative scores
        val node =
            createTestNode(
                score = 10,
                thresholds = listOf(Threshold("threshold", 50)),
            ) // Create normal node first
        node.result = KpiCalculationResult.Success(-10) // Then manually set negative score

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then it should return an error
        assertTrue(result is KpiCalculationResult.Error)
        assertTrue(result.reason.contains("Score for node"))
        assertTrue(result.reason.contains("is negative"))
    }

    @Test
    fun testTransformTechLagComponentWithMultipleThresholds() {
        // Given a node with multiple thresholds (should use the highest)
        val thresholds =
            listOf(
                Threshold("threshold1", 20),
                Threshold("threshold3", 60), // This should be the highest
                Threshold("threshold2", 40),
            )
        val node = createTestNode(score = 30, thresholds = thresholds)

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then it should use highest threshold (60) and score should be 100 (30 <= 60)
        assertTrue(result is KpiCalculationResult.Success)
        assertEquals(100, result.score)
    }

    @Test
    fun testTransformTechLagComponentScoreAtDoubleThreshold() {
        // Given a node with score exactly at 2*threshold
        val node = createTestNode(score = 100, thresholds = listOf(Threshold("threshold", 50)))

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then score should be 0 (boundary case - score = 2*threshold should be 0)
        assertTrue(result is KpiCalculationResult.Success)
        // Expected: score = 100, threshold = 50, so 100 = 2*50, which should be 0
        assertEquals(0, result.score)
    }

    @Test
    fun testTransformTechLagComponentWithIncompleteResult() {
        // Given a node with incomplete result
        val node = createTestNode(score = 30, thresholds = listOf(Threshold("threshold", 50)))
        node.result = KpiCalculationResult.Incomplete(30, "incomplete")

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then it should still work with the score from incomplete result
        assertTrue(result is KpiCalculationResult.Success)
        assertEquals(100, result.score) // 30 <= 50, so score should be 100
    }

    @Test
    fun testTransformTechLagComponentWithEmptyResult() {
        // Given a node with empty result (score = 0)
        val node = createTestNode(score = 0, thresholds = listOf(Threshold("threshold", 50)))
        node.result = KpiCalculationResult.Empty("empty result")

        // When transforming
        val result = DefaultRawValueTransformer.transformTechLagComponent(node)

        // Then it should work with score = 0
        assertTrue(result is KpiCalculationResult.Success)
        assertEquals(100, result.score) // 0 <= 50, so score should be 100
    }
}
