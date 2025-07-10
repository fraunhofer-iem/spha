/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi.hierarchy

import de.fraunhofer.iem.spha.model.assertEquals
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class JsonKpiResultHierarchyTest {

    @Test
    fun serializeDeserializeKpiEdge() {
        val node =
            KpiResultNode(
                typeId = "someId",
                result = KpiCalculationResult.Success(42),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )
        val edge = KpiResultEdge(actualWeight = 0.5, plannedWeight = 1.0, target = node)
        val newEdge =
            Json.Default.decodeFromString<KpiResultEdge>(Json.Default.encodeToString(edge))

        kotlin.test.assertEquals(edge, newEdge)
    }

    @ParameterizedTest
    @MethodSource("testKpiResultNodes")
    fun serializeDeserializeKpiNode(resultNode: KpiResultNode) {
        val newResultNode =
            Json.Default.decodeFromString<KpiResultNode>(Json.Default.encodeToString(resultNode))

        resultNode.assertEquals(newResultNode)
    }

    // This test doesn't really test any functionality. It's mainly here as a reminder to fail
    // whenever we change something
    // related to our external data model (the KpiResultHierarchy), as this is what the library
    // users
    // store and use to call the
    // library with.
    // TLDR; Whenever this test fails, we have a breaking change in how we construct our KPI
    //  hierarchy, meaning we potentially break our clients' code.
    @Test
    fun serializeResultHierarchyToExpectedJson() {
        val root =
            KpiResultNode(
                typeId = KpiType.ROOT.name,
                result = KpiCalculationResult.Success(100),
                strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                id = "rootId", // Stable ID here
                edges =
                    listOf(
                        KpiResultEdge(
                            plannedWeight = 1.0,
                            actualWeight = 0.5,
                            target =
                                KpiResultNode(
                                    typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                                    result = KpiCalculationResult.Success(100),
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = emptyList(),
                                    id = "cveId", // Stable ID here
                                    tags = setOf("A", "B", "a", "b", "A"),
                                    originId = "someOrigin",
                                    reason = "CRA relevant",
                                    thresholds = mapOf("warning" to 50, "critical" to 90)
                                ),
                        )
                    ),
            )

        val hierarchy = KpiResultHierarchy.create(root)

        val jsonResult = Json.Default.encodeToString(hierarchy)

        println(jsonResult)

        // Pretty printing might add spaces, tabs, (CR)LF, etc. which is hard to assert against.
        // This implicitly asserts that KpiResultNode and KpiResultEdge get serialized to the
        // expected JSON too
        val expected =
            "{\"root\":{\"typeId\":\"ROOT\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":100},\"strategy\":\"MAXIMUM_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":100},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[],\"tags\":[\"A\",\"B\",\"a\",\"b\"],\"originId\":\"someOrigin\",\"reason\":\"CRA relevant\",\"thresholds\":{\"warning\":50,\"critical\":90},\"id\":\"cveId\"},\"plannedWeight\":1.0,\"actualWeight\":0.5}],\"id\":\"rootId\"},\"schemaVersion\":\"1.1.0\",\"timestamp\":\"${hierarchy.timestamp}\"}"

        kotlin.test.assertEquals(expected, jsonResult)
    }

    companion object {
        @JvmStatic
        fun testKpiResultNodes() =
            listOf(
                KpiResultNode(
                    typeId = "someId",
                    result = KpiCalculationResult.Success(42),
                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                    edges = listOf(),
                    thresholds = mapOf()
                ),
                KpiResultNode(
                    typeId = "someId",
                    result = KpiCalculationResult.Error("someError"),
                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                    edges = listOf(),
                    id = "someId",
                    tags = setOf("A", "B", "a", "b", "A"),
                    originId = "someOrigin",
                    reason = "someReason",
                    thresholds = mapOf("warning" to 50, "critical" to 90)
                ),
                KpiResultNode(
                    typeId = "someId",
                    result = KpiCalculationResult.Incomplete(42, "someIncompleteReason"),
                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                    edges = listOf(),
                    id = "someId",
                ),
                KpiResultNode(
                    typeId = "someId",
                    result = KpiCalculationResult.Empty(),
                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                    edges = listOf(),
                    id = "someId",
                ),
            )
    }
}
