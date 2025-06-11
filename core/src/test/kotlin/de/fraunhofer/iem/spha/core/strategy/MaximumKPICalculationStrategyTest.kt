/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
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

class MaximumKPICalculationStrategyTest {

    @Test
    fun calculateEmpty() {

        val calcRelaxed =
            MaximumKPICalculationStrategy.calculateKpi(hierarchyEdges = listOf(), strict = false)
        val calcStrict =
            MaximumKPICalculationStrategy.calculateKpi(hierarchyEdges = listOf(), strict = true)

        assertEquals(true, calcRelaxed is KpiCalculationResult.Empty)
        assertEquals(true, calcStrict is KpiCalculationResult.Empty)
    }

    @Test
    fun calculateCorrect() {
        val root =
            KpiHierarchyNode.from(
                KpiNode(
                    typeId = KpiType.ROOT.name,
                    strategy = KpiStrategyId.MAXIMUM_STRATEGY,
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
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = 15),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 20),
                ),
            )

        val calcRelaxed = MaximumKPICalculationStrategy.calculateKpi(root.edges, strict = false)
        val calcStrict = MaximumKPICalculationStrategy.calculateKpi(root.edges, strict = true)

        assert(calcRelaxed is KpiCalculationResult.Success)
        assert(calcStrict is KpiCalculationResult.Success)
        assertEquals(20, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(20, (calcRelaxed as KpiCalculationResult.Success).score)
    }
}
