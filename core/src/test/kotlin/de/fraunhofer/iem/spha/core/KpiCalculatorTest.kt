/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.DefaultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import de.fraunhofer.iem.spha.model.kpi.hierarchy.SCHEMA_VERSIONS
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class KpiCalculatorTest {

    @Test
    fun calculateDefaultHierarchyKpis() {
        assertDoesNotThrow {
            val rawValueKpis =
                listOf(
                    RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 8),
                    RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 9),
                    RawValueKpi(typeId = KpiType.CHECKED_IN_BINARIES.name, score = 100),
                    RawValueKpi(typeId = KpiType.COMMENTS_IN_CODE.name, score = 80),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 90),
                    RawValueKpi(typeId = KpiType.IS_DEFAULT_BRANCH_PROTECTED.name, score = 100),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = 80),
                    RawValueKpi(typeId = KpiType.SECRETS.name, score = 80),
                    RawValueKpi(typeId = KpiType.SAST_USAGE.name, score = 80),
                    RawValueKpi(typeId = KpiType.DOCUMENTATION_INFRASTRUCTURE.name, score = 80),
                )
            KpiCalculator.calculateKpis(DefaultHierarchy.get(), rawValueKpis)
        }
    }

    @Test
    fun calculateMaxKpis() {
        val rawValueKpis =
            listOf(
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 82),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 90),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 65),
            )

        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.MAXIMAL_VULNERABILITY.name,
                                    strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId =
                                                            KpiType.CODE_VULNERABILITY_SCORE.name,
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                        edges = emptyList(),
                                                    ),
                                                weight = 1.0,
                                            )
                                        ),
                                ),
                            weight = 1.0,
                        )
                    ),
            )
        val hierarchy = KpiHierarchy.create(root)

        val res = KpiCalculator.calculateKpis(hierarchy, rawValueKpis)
        val result = res.rootNode.result

        if (result is KpiCalculationResult.Success) {
            assertEquals(90, result.score)
        } else {
            fail()
        }
    }

    @Test
    fun calculateMaxKpisIncomplete() {
        val rawValueKpis =
            listOf(
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 82),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 90),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 65),
            )

        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.SECURITY.name,
                                    strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId =
                                                            KpiType.CODE_VULNERABILITY_SCORE.name,
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                        edges = emptyList(),
                                                    ),
                                                weight = 1.0,
                                            ),
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId = KpiType.SAST_USAGE.name,
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                        edges = emptyList(),
                                                    ),
                                                weight = 1.0,
                                            ),
                                        ),
                                ),
                            weight = 1.0,
                        )
                    ),
            )
        val hierarchy = KpiHierarchy.create(root)

        val res = KpiCalculator.calculateKpis(hierarchy, rawValueKpis)
        val result = res.rootNode.result

        if (result is KpiCalculationResult.Incomplete) {
            assertEquals(90, result.score)
        } else {
            fail()
        }
    }

    @Test
    fun calculateRatioKpisIncomplete() {
        val rawValueKpis =
            listOf(
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 82),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 90),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 65),
            )

        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.SIGNED_COMMITS_RATIO.name,
                                    strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId = KpiType.NUMBER_OF_COMMITS.name,
                                                        edges = emptyList(),
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                    ),
                                                weight = 1.0,
                                            ),
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId =
                                                            KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                                                        edges = emptyList(),
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                    ),
                                                weight = 1.0,
                                            ),
                                        ),
                                ),
                            weight = 0.5,
                        ),
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.SECURITY.name,
                                    strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId =
                                                            KpiType.CODE_VULNERABILITY_SCORE.name,
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                        edges = emptyList(),
                                                    ),
                                                weight = 0.5,
                                            )
                                        ),
                                ),
                            weight = 0.5,
                        ),
                    ),
            )

        val hierarchy = KpiHierarchy.create(root)

        val res = KpiCalculator.calculateKpis(hierarchy, rawValueKpis)
        val result = res.rootNode.result

        if (result is KpiCalculationResult.Incomplete) {
            assertEquals(90, result.score)
        } else {
            fail()
        }
    }

    @Test
    fun calculateAggregation() {
        val rawValueKpis =
            listOf(
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 80),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 90),
            )

        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = listOf(),
                                ),
                            weight = 1.0,
                        )
                    ),
            )
        val hierarchy = KpiHierarchy.create(root)

        val res = KpiCalculator.calculateKpis(hierarchy, rawValueKpis)
        val result = res.rootNode.result

        if (result is KpiCalculationResult.Success) {
            assertEquals(85, result.score)
        } else {
            fail()
        }
    }

    @Test
    fun calculateAggregationKpisIncompleteMixedKpiIds() {
        val rawValueKpis =
            listOf(
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 80),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 90),
            )

        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = listOf(),
                                ),
                            weight = 0.5,
                        ),
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.SAST_USAGE.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = listOf(),
                                ),
                            weight = 0.5,
                        ),
                    ),
            )
        val hierarchy = KpiHierarchy.create(root)

        val res = KpiCalculator.calculateKpis(hierarchy, rawValueKpis)
        val result = res.rootNode.result

        if (result is KpiCalculationResult.Incomplete) {
            assertEquals(85, result.score)
            val sastResult =
                res.rootNode.children.find { it.target.typeId == KpiType.SAST_USAGE.name } ?: fail()
            assertEquals(0.0, sastResult.actualWeight)
            val vulnerabilityEdges =
                res.rootNode.children.filter {
                    it.target.typeId == KpiType.CODE_VULNERABILITY_SCORE.name
                }
            assertEquals(2, vulnerabilityEdges.size)
            assertEquals(vulnerabilityEdges.first().actualWeight, 0.5)
            assertEquals(vulnerabilityEdges[1].actualWeight, 0.5)
        } else {
            fail()
        }
    }

    @Test
    fun calculateAggregationKpisIncompleteMixedkpiIdsNested() {
        val rawValueKpis =
            listOf(
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 80),
                RawValueKpi(typeId = KpiType.CODE_VULNERABILITY_SCORE.name, score = 90),
            )

        val root =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.MAXIMAL_VULNERABILITY.name,
                                    strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId =
                                                            KpiType.CODE_VULNERABILITY_SCORE.name,
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                        edges = listOf(),
                                                    ),
                                                weight = 1.0,
                                            )
                                        ),
                                ),
                            weight = 0.5,
                        ),
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.SAST_USAGE.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = listOf(),
                                ),
                            weight = 0.5,
                        ),
                    ),
            )
        val hierarchy = KpiHierarchy.create(root)

        val res = KpiCalculator.calculateKpis(hierarchy, rawValueKpis)
        val result = res.rootNode.result

        if (result is KpiCalculationResult.Incomplete) {
            assertEquals(90, result.score)
            val sastResult =
                res.rootNode.children.find { it.target.typeId == KpiType.SAST_USAGE.name } ?: fail()
            assertEquals(0.0, sastResult.actualWeight)
            val vulnerabilityEdges =
                res.rootNode.children.filter {
                    it.target.typeId == KpiType.MAXIMAL_VULNERABILITY.name
                }
            assertEquals(vulnerabilityEdges.first().actualWeight, 1.0)
            assertEquals(vulnerabilityEdges.first().plannedWeight, 0.5)
        } else {
            fail()
        }
    }

    @Test
    fun calculatePreservesPropertiesFromInputData() {
        val cvssValue = RawValueKpi(
            typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
            score = 42,
            originId = "cvssOrigin"
        )
        val sastValue = RawValueKpi(
            typeId = KpiType.SAST_USAGE.name,
            score = 42,
            originId = "sastOrigin"
        )

        val sastNode = KpiNode(
            typeId = KpiType.SAST_USAGE.name,
            strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
            edges = emptyList(),
        )
        val cvssNode = KpiNode(
            typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
            strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
            edges = emptyList(),
            tags = setOf("cvss", "cve", "cwe"),
            reason = "CRA relevant"
        )

        val root = KpiNode(
            typeId = KpiType.ROOT.name,
            strategy = KpiStrategyId.MAXIMUM_STRATEGY,
            edges = listOf(
                KpiEdge(sastNode, 1.0),
                KpiEdge(cvssNode, 1.0)
            )
        )
        val hierarchy = KpiHierarchy.create(root)
        val result = KpiCalculator.calculateKpis(hierarchy, listOf(cvssValue, sastValue).shuffled())

        assertEquals(SCHEMA_VERSIONS.last(), result.schemaVersion)

        // Check Root Node
        assertEquals(setOf(), result.rootNode.tags)
        assertNull(result.rootNode.originId)
        assertNull(result.rootNode.reason)
        assertEquals(2, result.rootNode.children.count())
        assertFalse { result.rootNode.id == sastValue.id || result.rootNode.id == cvssValue.id } // Ensures id is not inherited

        // Check CVSS Node
        val cvssResultNode = result.rootNode.children.first { it.target.typeId ==  KpiType.CODE_VULNERABILITY_SCORE.name }.target
        assertEquals(0, cvssResultNode.children.count())
        assertEquals(KpiStrategyId.RAW_VALUE_STRATEGY, cvssResultNode.strategy)
        assertEquals(cvssValue.id, cvssResultNode.id)
        assertEquals(setOf(), cvssResultNode.tags)
        assertEquals("cvssOrigin", cvssResultNode.originId)
        assertEquals("CRA relevant", cvssResultNode.reason)

        // Check SAST Node
        val sastResultNode = result.rootNode.children.first { it.target.typeId ==  KpiType.SAST_USAGE.name }.target
        assertEquals(0, sastResultNode.children.count())
        assertEquals(KpiStrategyId.RAW_VALUE_STRATEGY, sastResultNode.strategy)
        assertEquals(sastValue.id, sastResultNode.id)
        assertEquals(setOf(), sastResultNode.tags)
        assertEquals("sastOrigin", sastResultNode.originId)
        assertNull(sastResultNode.reason)
    }
}
