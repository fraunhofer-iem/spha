/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
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
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

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
                tags = setOf("tag1", "tag2"),
                reason = "someReason",
            )
        assertEquals("ROOT", resultNode.typeId)
        assertEquals(KpiStrategyId.RAW_VALUE_STRATEGY, resultNode.strategy)
        assertEquals("someReason", resultNode.reason)
        assertEquals(setOf("tag1", "tag2"), resultNode.tags)
    }

    @Test
    fun kpiNodeCtorDefaults() {
        val node =
            KpiNode(
                KpiStrategyId.RAW_VALUE_STRATEGY.name,
                KpiStrategyId.RAW_VALUE_STRATEGY,
                listOf(),
            )

        assertEquals(setOf(), node.tags)
        assertNull(node.reason)
    }

    @Test
    fun constructHierarchy() {
        // This test doesn't really test any functionality. It's mainly here as a reminder to fail
        // whenever we change something
        // related to our external data model (the KpiHierarchy), as this is what the library users
        // store and use to call the
        // library with.
        // TLDR; Whenever this test fails, we have a breaking change in how we construct our KPI
        //  hierarchy, meaning we potentially break our clients' code.
        assertDoesNotThrow {
            val childNodes =
                listOf(
                    KpiEdge(
                        KpiNode(
                            typeId = KpiType.SECURITY.name,
                            KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                            listOf(),
                            reason = "Security reason",
                            tags = setOf("security", "critical"),
                        ),
                        weight = 0.3,
                    ),
                    KpiEdge(
                        KpiNode(
                            typeId = KpiType.PROCESS_COMPLIANCE.name,
                            KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                            listOf(),
                            reason = null,
                            tags = setOf("compliance"),
                        ),
                        weight = 0.3,
                    ),
                    KpiEdge(
                        KpiNode(
                            typeId = KpiType.INTERNAL_QUALITY.name,
                            KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                            listOf(),
                            reason = null,
                            tags = emptySet(),
                        ),
                        weight = 0.3,
                    ),
                )
            val root =
                KpiNode(
                    typeId = KpiType.ROOT.name,
                    strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                    edges = childNodes,
                    reason = "Root reason",
                    tags = setOf("root", "summary"),
                )
            val hierarchy = KpiHierarchy.create(root)

            val json = Json { prettyPrint = true }
            val jsonResult = json.encodeToString(hierarchy)

            println(jsonResult)
            assertEquals(hierarchy.schemaVersion, "1.1.0")
            // Check that the reason is present in the serialized output
            assert(jsonResult.contains("Root reason"))
            assert(jsonResult.contains("summary"))
            assert(jsonResult.contains("Security reason"))
            // Ensure that 'reason = null' is not present in the serialized output
            assert(!jsonResult.contains("\"reason\":null"))
        }
    }
}
