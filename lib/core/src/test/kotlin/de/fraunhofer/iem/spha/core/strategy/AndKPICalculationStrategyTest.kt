/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.strategy

import de.fraunhofer.iem.spha.core.hierarchy.KpiHierarchyNode
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import kotlin.test.Test
import org.junit.jupiter.api.Assertions.assertEquals

class AndKPICalculationStrategyTest {

    @Test
    fun emptyEdges() {
        assertEquals(
            KpiCalculationResult.Empty(),
            AndKPICalculationStrategy.calculateKpi(listOf(), strict = true),
        )
        assertEquals(
            KpiCalculationResult.Empty(),
            AndKPICalculationStrategy.calculateKpi(listOf(), strict = false),
        )
    }

    @Test
    fun calculateCorrectFalse() {
        val root =
            KpiHierarchyNode.from(
                KpiNode(
                    typeId = KpiType.ROOT.name,
                    strategy = KpiStrategyId.XOR_STRATEGY,
                    edges =
                        listOf(
                            KpiEdge(
                                target =
                                    KpiNode(
                                        typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                        edges = listOf(),
                                    ),
                                weight = 0.5,
                            ),
                            KpiEdge(
                                target =
                                    KpiNode(
                                        typeId = KpiType.NUMBER_OF_COMMITS.name,
                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                        edges = listOf(),
                                    ),
                                weight = 0.5,
                            ),
                        ),
                ),
                listOf(
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = 100),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 20),
                ),
            )

        val calcRelaxed =
            AndKPICalculationStrategy.calculateKpi(hierarchyEdges = root.edges, strict = false)

        val calcStrict =
            AndKPICalculationStrategy.calculateKpi(hierarchyEdges = root.edges, strict = true)

        assertEquals(true, calcStrict is KpiCalculationResult.Success)
        assertEquals(true, calcRelaxed is KpiCalculationResult.Success)

        assertEquals(0, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(0, (calcRelaxed as KpiCalculationResult.Success).score)
    }

    @Test
    fun calculateCorrect() {
        val root =
            KpiHierarchyNode.from(
                KpiNode(
                    typeId = KpiType.ROOT.name,
                    strategy = KpiStrategyId.XOR_STRATEGY,
                    edges =
                        listOf(
                            KpiEdge(
                                target =
                                    KpiNode(
                                        typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                        edges = listOf(),
                                    ),
                                weight = 0.5,
                            ),
                            KpiEdge(
                                target =
                                    KpiNode(
                                        typeId = KpiType.NUMBER_OF_COMMITS.name,
                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                        edges = listOf(),
                                    ),
                                weight = 0.5,
                            ),
                        ),
                ),
                listOf(
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = 100),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 100),
                ),
            )

        val calcRelaxed =
            AndKPICalculationStrategy.calculateKpi(hierarchyEdges = root.edges, strict = false)

        val calcStrict =
            AndKPICalculationStrategy.calculateKpi(hierarchyEdges = root.edges, strict = true)

        assertEquals(true, calcStrict is KpiCalculationResult.Success)
        assertEquals(true, calcRelaxed is KpiCalculationResult.Success)

        assertEquals(100, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(100, (calcRelaxed as KpiCalculationResult.Success).score)
    }
}
