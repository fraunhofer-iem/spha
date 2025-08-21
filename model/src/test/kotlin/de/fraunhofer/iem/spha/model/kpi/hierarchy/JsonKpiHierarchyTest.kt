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
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class JsonKpiHierarchyTest {

    @Test
    fun serializeDeserializeKpiEdge() {
        val node =
            KpiNode(
                typeId = "someId",
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = listOf(),
            )
        val edge = KpiEdge(node, 0.789)
        val newEdge = Json.decodeFromString<KpiEdge>(Json.encodeToString(edge))

        assertEquals(edge, newEdge)
    }

    @ParameterizedTest
    @MethodSource("testKpiNodes")
    fun serializeDeserializeKpiNode(node: KpiNode) {
        val newNode = Json.decodeFromString<KpiNode>(Json.encodeToString(node))

        node.assertEquals(newNode)
    }

    @Test
    fun serializeDeserializeDefaultHierarchy() {
        val defaultHierarchy = DefaultHierarchy.get()

        val hierarchy = Json.decodeFromString<KpiHierarchy>(Json.encodeToString(defaultHierarchy))

        assertEquals(defaultHierarchy, hierarchy)
    }

    // This test doesn't really test any functionality. It's mainly here as a reminder to fail
    // whenever we change something
    // related to our external data model (the KpiHierarchy), as this is what the library users
    // store and use to call the
    // library with.
    // TLDR; Whenever this test fails, we have a breaking change in how we construct our KPI
    //  hierarchy, meaning we potentially break our clients' code.
    @Test
    fun serializeHierarchyToExpectedJson() {
        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            weight = 1.0,
                            target =
                                KpiNode(
                                    typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = emptyList(),
                                    metaInfo =
                                        MetaInfo(
                                            tags = setOf("cvss", "cve", "cwe"),
                                            description = "CRA relevant",
                                        ),
                                    thresholds =
                                        listOf(Threshold("warning", 50), Threshold("critical", 90)),
                                ),
                        )
                    ),
            )

        val jsonResult = Json.encodeToString(KpiHierarchy.create(root))

        // Pretty printing might add spaces, tabs, (CR)LF, etc. which is hard to assert against.
        // This implicitly asserts that KpiNode and KpiEdge get serialized to the expected JSON too
        val expected =
            "{\"root\":{\"typeId\":\"ROOT\",\"strategy\":\"MAXIMUM_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"strategy\":\"RAW_VALUE_STRATEGY\",\"thresholds\":[{\"name\":\"warning\",\"value\":50},{\"name\":\"critical\",\"value\":90}],\"metaInfo\":{\"description\":\"CRA relevant\",\"tags\":[\"cvss\",\"cve\",\"cwe\"]}},\"weight\":1.0}]},\"schemaVersion\":\"1.1.0\"}"

        assertEquals(expected, jsonResult)
    }

    companion object {
        @JvmStatic
        fun testKpiNodes() =
            listOf(
                KpiNode(
                    typeId = "someId",
                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                    edges = listOf(),
                    thresholds = listOf(),
                ),
                KpiNode(
                    typeId = "someId",
                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                    edges = listOf(),
                    metaInfo =
                        MetaInfo(tags = setOf("A", "B", "a", "b", "A"), description = "someReason"),
                    thresholds = listOf(Threshold("warning", 50), Threshold("critical", 90)),
                ),
            )
    }
}
