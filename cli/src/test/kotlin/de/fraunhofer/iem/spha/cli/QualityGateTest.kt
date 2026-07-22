/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QualityGateTest {

    private fun leaf(typeId: String, result: KpiCalculationResult) =
        KpiResultNode(typeId, result, KpiStrategyId.RAW_VALUE_STRATEGY, emptyList())

    private fun hierarchy(
        rootResult: KpiCalculationResult,
        children: List<KpiResultNode> = emptyList(),
        rootTypeId: String = "ROOT",
    ): KpiResultHierarchy =
        KpiResultHierarchy.create(
            KpiResultNode(
                rootTypeId,
                rootResult,
                KpiStrategyId.AND_STRATEGY,
                children.map { KpiResultEdge(it, plannedWeight = 1.0, actualWeight = 1.0) },
            )
        )

    @Test
    fun testPass_rootSuccessAtThreshold() {
        val h = hierarchy(KpiCalculationResult.Success(100))
        val result = QualityGate.evaluate(h, requiredNodeTypeIds = emptyList(), minScore = 100)
        assertTrue(result.passed)
        assertEquals(listOf("ROOT"), result.verdicts.map { it.typeId })
    }

    @Test
    fun testFail_belowThreshold() {
        val h = hierarchy(KpiCalculationResult.Success(80))
        val result = QualityGate.evaluate(h, requiredNodeTypeIds = emptyList(), minScore = 100)
        assertFalse(result.passed)
    }

    @Test
    fun testFail_emptyLeaf() {
        // Missing raw data for a required leaf ⇒ Empty ⇒ must fail (fail-closed), not pass.
        val leaf = leaf("SBOM_FRESHNESS", KpiCalculationResult.Empty())
        val h = hierarchy(KpiCalculationResult.Success(100), children = listOf(leaf))
        val result =
            QualityGate.evaluate(h, requiredNodeTypeIds = listOf("SBOM_FRESHNESS"), minScore = 100)
        assertFalse(result.passed)
    }

    @Test
    fun testFail_errorNode() {
        val leaf = leaf("B1", KpiCalculationResult.Error("computation failed"))
        val h = hierarchy(KpiCalculationResult.Success(100), children = listOf(leaf))
        val result = QualityGate.evaluate(h, requiredNodeTypeIds = listOf("B1"), minScore = 100)
        assertFalse(result.passed)
    }

    @Test
    fun testFail_unknownRequireNode() {
        val h = hierarchy(KpiCalculationResult.Success(100))
        val result =
            QualityGate.evaluate(h, requiredNodeTypeIds = listOf("DOES_NOT_EXIST"), minScore = 100)
        assertFalse(result.passed)
        assertTrue(result.verdicts.single().detail.contains("absent"))
    }

    @Test
    fun testPass_allRequiredChildNodesPass() {
        val b1 = leaf("B1", KpiCalculationResult.Success(100))
        val b2 = leaf("B2", KpiCalculationResult.Success(100))
        // A root that itself is not passing must NOT drag the gate down when only the leaves are
        // required — the gate evaluates exactly the nodes asked for.
        val h = hierarchy(KpiCalculationResult.Success(0), children = listOf(b1, b2))
        val result =
            QualityGate.evaluate(h, requiredNodeTypeIds = listOf("B1", "B2"), minScore = 100)
        assertTrue(result.passed)
        assertEquals(2, result.verdicts.size)
    }

    @Test
    fun testFail_oneRequiredChildFails() {
        val b1 = leaf("B1", KpiCalculationResult.Success(100))
        val b2 = leaf("B2", KpiCalculationResult.Success(0))
        val h = hierarchy(KpiCalculationResult.Success(0), children = listOf(b1, b2))
        val result =
            QualityGate.evaluate(h, requiredNodeTypeIds = listOf("B1", "B2"), minScore = 100)
        assertFalse(result.passed)
        assertEquals(false, result.verdicts.single { it.typeId == "B2" }.passed)
        assertEquals(true, result.verdicts.single { it.typeId == "B1" }.passed)
    }

    @Test
    fun testCustomMinScore() {
        val h = hierarchy(KpiCalculationResult.Success(90))
        assertTrue(QualityGate.evaluate(h, emptyList(), minScore = 90).passed)
        assertFalse(QualityGate.evaluate(h, emptyList(), minScore = 91).passed)
    }
}
