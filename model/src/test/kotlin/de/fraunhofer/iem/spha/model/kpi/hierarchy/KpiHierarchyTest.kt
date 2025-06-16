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
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class KpiHierarchyTest {
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
                            tags = listOf("security", "critical"),
                        ),
                        weight = 0.3,
                    ),
                    KpiEdge(
                        KpiNode(
                            typeId = KpiType.PROCESS_COMPLIANCE.name,
                            KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                            listOf(),
                            reason = null,
                            tags = listOf("compliance"),
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
                    tags = listOf("root", "summary"),
                )
            val hierarchy = KpiHierarchy.create(root)

            val json = Json { prettyPrint = true }
            val jsonResult = json.encodeToString(hierarchy)

            println(jsonResult)
            assertEquals(hierarchy.schemaVersion, "1.1.0")
            // Check that the reason is present in the serialized output
            assert(jsonResult.contains("Root reason"))
            assert(jsonResult.contains("Security reason"))
            // Check that tags are present in the serialized output
            assert(jsonResult.contains("root"))
            assert(jsonResult.contains("security"))
            assert(jsonResult.contains("critical"))
            assert(jsonResult.contains("compliance"))
        }
    }
}
