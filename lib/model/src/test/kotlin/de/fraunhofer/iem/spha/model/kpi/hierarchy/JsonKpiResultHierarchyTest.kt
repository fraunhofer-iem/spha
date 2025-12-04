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
            Json.decodeFromString<KpiResultEdge>(Json.encodeToString(edge))

        kotlin.test.assertEquals(edge, newEdge)
    }

    @ParameterizedTest
    @MethodSource("testKpiResultNodes")
    fun serializeDeserializeKpiNode(resultNode: KpiResultNode) {
        val newResultNode =
            Json.decodeFromString<KpiResultNode>(Json.encodeToString(resultNode))

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
                                    metaInfo =
                                        MetaInfo(
                                            tags = setOf("A", "B", "a", "b", "A"),
                                            description = "CRA relevant",
                                        ),
                                    originId = "someOrigin",
                                    thresholds =
                                        listOf(Threshold("warning", 50), Threshold("critical", 90)),
                                ),
                        )
                    ),
            )

        val hierarchy = KpiResultHierarchy.create(root)

        val jsonResult = Json.encodeToString(hierarchy)

        println("Actual JSON: $jsonResult")

        // Pretty printing might add spaces, tabs, (CR)LF, etc. which is hard to assert against.
        // This implicitly asserts that KpiResultNode and KpiResultEdge get serialized to the
        // expected JSON too
        val expected =
            "{\"root\":{\"typeId\":\"ROOT\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":100},\"strategy\":\"MAXIMUM_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":100},\"strategy\":\"RAW_VALUE_STRATEGY\",\"thresholds\":[{\"name\":\"warning\",\"value\":50},{\"name\":\"critical\",\"value\":90}],\"originId\":\"someOrigin\",\"metaInfo\":{\"description\":\"CRA relevant\",\"tags\":[\"A\",\"B\",\"a\",\"b\"]},\"id\":\"cveId\"},\"plannedWeight\":1.0,\"actualWeight\":0.5}],\"id\":\"rootId\"},\"schemaVersion\":\"1.1.0\",\"timestamp\":\"${hierarchy.timestamp}\"}"

        println("Expected JSON: $expected")

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
                    thresholds = listOf(),
                ),
                KpiResultNode(
                    typeId = "someId",
                    result = KpiCalculationResult.Error("someError"),
                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                    edges = listOf(),
                    id = "someId",
                    originId = "someOrigin",
                    metaInfo =
                        MetaInfo(tags = setOf("A", "B", "a", "b", "A"), description = "someReason"),
                    thresholds = listOf(Threshold("warning", 50), Threshold("critical", 90)),
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
