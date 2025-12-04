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
import de.fraunhofer.iem.spha.model.kpi.KpiType
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.jupiter.api.Test

class KpiHierarchyTest {

    @Test
    fun createWithSamePropertiesAndLatestVersion() {
        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = emptyList(),
                                ),
                            weight = 1.0,
                        ),
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.NUMBER_OF_COMMITS.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = emptyList(),
                                ),
                            weight = 1.0,
                        ),
                    ),
            )
        val hierarchy = KpiHierarchy.Companion.create(root)

        assertSame(root, hierarchy.root)
        assertEquals(SCHEMA_VERSIONS.last(), hierarchy.schemaVersion)
    }

    @Test
    fun kpiEdgeCtorSetsProperties() {
        val node =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )
        val edge = KpiEdge(weight = 0.5, target = node)

        assertSame(node, edge.target)
        assertEquals(0.5, edge.weight)
    }

    @Test
    fun kpiNodeCtorSetsProperties() {
        val resultNode =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
                metaInfo =
                    MetaInfo(tags = setOf("A", "B", "a", "b", "A"), description = "someReason"),
                thresholds = listOf(Threshold("warning", 50), Threshold("critical", 90)),
            )
        assertEquals("ROOT", resultNode.typeId)
        assertEquals(KpiStrategyId.RAW_VALUE_STRATEGY, resultNode.strategy)
        assertEquals("someReason", resultNode.metaInfo?.description)
        assertEquals(setOf("A", "B", "a", "b"), resultNode.metaInfo?.tags)
        assertEquals(
            listOf(Threshold("warning", 50), Threshold("critical", 90)),
            resultNode.thresholds,
        )
    }

    @Test
    fun kpiNodeCtorDefaults() {
        val node =
            KpiNode(
                KpiStrategyId.RAW_VALUE_STRATEGY.name,
                KpiStrategyId.RAW_VALUE_STRATEGY,
                listOf(),
            )

        assertEquals(listOf(), node.thresholds)
        assertNull(node.metaInfo)
    }
}
