/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi.hierarchy

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.jupiter.api.Test

class KpiResultHierarchyTest {

    @Test
    fun createWithSamePropertiesAndLatestVersion() {
        val root =
            KpiResultNode(
                typeId = KpiType.ROOT.name,
                result = KpiCalculationResult.Error("failed"),
                strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                children =
                    listOf(
                        KpiResultEdge(
                            plannedWeight = 1.0,
                            actualWeight = 1.0,
                            target =
                                KpiResultNode(
                                    typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                                    result = KpiCalculationResult.Success(42),
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    children = listOf(),
                                ),
                        )
                    ),
            )
        val resultHierarchy = KpiResultHierarchy.create(root)
        assertSame(root, resultHierarchy.root)
        assertEquals(SCHEMA_VERSIONS.last(), resultHierarchy.schemaVersion)
    }

    @Test
    fun kpiResultNodeCtorDefaults() {
        val resultNode =
            KpiResultNode(
                typeId = KpiType.ROOT.name,
                result = KpiCalculationResult.Success(42),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                children = listOf(),
            )
        assertEquals(setOf(), resultNode.tags)
        assertNull(resultNode.reason)
        assertNull(resultNode.originId)
    }

    @Test
    fun kpiResultEdgeCtorSetsProperties() {
        val resultNode =
            KpiResultNode(
                typeId = KpiType.ROOT.name,
                result = KpiCalculationResult.Success(42),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                children = listOf(),
            )
        val resultEdge = KpiResultEdge(plannedWeight = 0.5, actualWeight = 1.0, target = resultNode)

        assertSame(resultNode, resultEdge.target)
        assertEquals(0.5, resultEdge.plannedWeight)
        assertEquals(1.0, resultEdge.actualWeight)
    }

    @Test
    fun kpiResultNodeCtorSetsProperties() {
        val resultNode =
            KpiResultNode(
                typeId = KpiType.ROOT.name,
                result = KpiCalculationResult.Success(42),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                children = listOf(),
                id = "someKpiId",
                originId = "someOrigin",
                tags = setOf("tag1", "tag2"),
            )
        assertEquals("ROOT", resultNode.typeId)
        assertEquals(KpiStrategyId.RAW_VALUE_STRATEGY, resultNode.strategy)
        val success = assertIs<KpiCalculationResult.Success>(resultNode.result)
        assertEquals(42, success.score)
        assertEquals("someKpiId", resultNode.id)
        assertEquals("someOrigin", resultNode.originId)
        assertEquals(setOf("tag1", "tag2"), resultNode.tags)
    }
}
