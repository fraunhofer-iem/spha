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
import de.fraunhofer.iem.spha.model.kpi.hierarchy.MetaInfo
import kotlin.test.Test
import org.junit.jupiter.api.Assertions.assertEquals

class MinimumKPICalculationStrategyTest {

    @Test
    fun calculateEmpty() {

        val calcRelaxed =
            MinimumKPICalculationStrategy.calculateKpi(hierarchyEdges = listOf(), strict = false)
        val calcStrict =
            MinimumKPICalculationStrategy.calculateKpi(hierarchyEdges = listOf(), strict = true)

        assertEquals(true, calcRelaxed is KpiCalculationResult.Empty)
        assertEquals(true, calcStrict is KpiCalculationResult.Empty)
    }

    @Test
    fun calculateCorrect() {
        val root =
            KpiHierarchyNode.from(
                KpiNode(
                    typeId = KpiType.ROOT.name,
                    strategy = KpiStrategyId.MINIMUM_STRATEGY,
                    edges =
                        listOf(
                            KpiEdge(
                                target =
                                    KpiNode(
                                        typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                        edges = listOf(),
                                        metaInfo = MetaInfo(description = "Signed commits reason"),
                                    ),
                                weight = 0.5,
                            ),
                            KpiEdge(
                                target =
                                    KpiNode(
                                        typeId = KpiType.NUMBER_OF_COMMITS.name,
                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                        edges = listOf(),
                                        metaInfo = null,
                                    ),
                                weight = 0.5,
                            ),
                        ),
                    metaInfo = MetaInfo(description = "Root minimum reason"),
                ),
                listOf(
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = 15),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 20),
                ),
            )

        // Check that the metaInfo description is present in the node
        assertEquals("Root minimum reason", root.metaInfo?.description)
        assertEquals("Signed commits reason", root.edges[0].to.metaInfo?.description)

        val calcRelaxed = MinimumKPICalculationStrategy.calculateKpi(root.edges, strict = false)
        val calcStrict = MinimumKPICalculationStrategy.calculateKpi(root.edges, strict = true)

        assert(calcRelaxed is KpiCalculationResult.Success)
        assert(calcStrict is KpiCalculationResult.Success)
        assertEquals(15, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(15, (calcRelaxed as KpiCalculationResult.Success).score)
    }
}
