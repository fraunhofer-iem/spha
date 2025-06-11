/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.strategy

import de.fraunhofer.iem.spha.core.KpiCalculator
import de.fraunhofer.iem.spha.core.hierarchy.KpiHierarchyNode
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeightedRatioKPICalculationStrategyTest {

    @Test
    fun isValidEmpty() {
        val node =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                edges = listOf(),
            )

        assertEquals(true, WeightedRatioKPICalculationStrategy.isValid(node = node, strict = false))
        assertEquals(true, WeightedRatioKPICalculationStrategy.isValid(node = node, strict = true))

        val incorrectStrategy =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges = listOf(),
            )

        assertEquals(
            true,
            WeightedRatioKPICalculationStrategy.isValid(node = incorrectStrategy, strict = false),
        )
        assertEquals(
            true,
            WeightedRatioKPICalculationStrategy.isValid(node = incorrectStrategy, strict = true),
        )
    }

    @Test
    fun calculateEmpty() {

        val calcRelaxed =
            WeightedRatioKPICalculationStrategy.calculateKpi(
                hierarchyEdges = listOf(),
                strict = false,
            )
        val calcStrict =
            WeightedRatioKPICalculationStrategy.calculateKpi(
                hierarchyEdges = listOf(),
                strict = true,
            )

        assertEquals(true, calcRelaxed is KpiCalculationResult.Empty)
        assertEquals(true, calcStrict is KpiCalculationResult.Empty)
    }

    @Test
    fun isValidCorrectChildren() {
        val nodeCorrectChildren =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.NUMBER_OF_COMMITS.name,
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
            )

        assertEquals(
            true,
            WeightedRatioKPICalculationStrategy.isValid(node = nodeCorrectChildren, strict = false),
        )
        assertEquals(
            true,
            WeightedRatioKPICalculationStrategy.isValid(node = nodeCorrectChildren, strict = true),
        )
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

        val calcRelaxed =
            WeightedRatioKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = false,
            )
        val calcStrict =
            WeightedRatioKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = true,
            )

        assertEquals(true, calcRelaxed is KpiCalculationResult.Success)
        assertEquals(true, calcRelaxed is KpiCalculationResult.Success)

        assertEquals(75, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(75, (calcRelaxed as KpiCalculationResult.Success).score)
    }

    @Test
    fun isValidToManyChildren() {
        val nodeManyChildren =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.NUMBER_OF_COMMITS.name,
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
            )

        assertEquals(
            true,
            WeightedRatioKPICalculationStrategy.isValid(node = nodeManyChildren, strict = false),
        )
        assertEquals(
            false,
            WeightedRatioKPICalculationStrategy.isValid(node = nodeManyChildren, strict = true),
        )

        val root =
            KpiHierarchyNode.from(
                nodeManyChildren,
                listOf(RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 15)),
            )

        val relaxed =
            WeightedRatioKPICalculationStrategy.calculateKpi(root.hierarchyEdges, strict = false)
        val strict =
            WeightedRatioKPICalculationStrategy.calculateKpi(root.hierarchyEdges, strict = true)
        assertEquals(true, relaxed is KpiCalculationResult.Error)
        assertEquals(true, strict is KpiCalculationResult.Error)
    }

    @Test
    fun nestedError() {
        val nestedError =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.NUMBER_OF_COMMITS.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = listOf(),
                                ),
                            weight = 0.5,
                        ),
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.SECURITY.name,
                                    strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                                    edges =
                                        listOf(
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId = KpiType.NUMBER_OF_COMMITS.name,
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                        edges = listOf(),
                                                    ),
                                                weight = 0.5,
                                            ),
                                            KpiEdge(
                                                target =
                                                    KpiNode(
                                                        typeId =
                                                            KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                                                        strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                                        edges = listOf(),
                                                    ),
                                                weight = 0.5,
                                            ),
                                        ),
                                ),
                            weight = 0.5,
                        ),
                    ),
            )

        val root =
            KpiHierarchyNode.from(
                nestedError,
                listOf(
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 0),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = -1),
                ),
            )

        KpiHierarchyNode.depthFirstTraversal(root) { it.result = KpiCalculator.calculateKpi(it) }

        assertEquals(true, root.result is KpiCalculationResult.Error)

        val relaxed =
            WeightedRatioKPICalculationStrategy.calculateKpi(root.hierarchyEdges, strict = false)
        val strict =
            WeightedRatioKPICalculationStrategy.calculateKpi(root.hierarchyEdges, strict = true)

        assertEquals(true, relaxed is KpiCalculationResult.Error)
        assertEquals(true, strict is KpiCalculationResult.Error)
        assertEquals(
            "Ratio calculation strategy has elements without result",
            (strict as KpiCalculationResult.Error).reason,
        )
        assertEquals(
            "Ratio calculation strategy has elements without result",
            (relaxed as KpiCalculationResult.Error).reason,
        )
    }

    @Test
    fun isValidToFewChildren() {
        val nodeToFewChildren =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.WEIGHTED_RATIO_STRATEGY,
                edges =
                    listOf(
                        KpiEdge(
                            target =
                                KpiNode(
                                    typeId = KpiType.NUMBER_OF_COMMITS.name,
                                    strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                    edges = listOf(),
                                ),
                            weight = 0.5,
                        )
                    ),
            )

        assertEquals(
            false,
            WeightedRatioKPICalculationStrategy.isValid(node = nodeToFewChildren, strict = false),
        )
        assertEquals(
            false,
            WeightedRatioKPICalculationStrategy.isValid(node = nodeToFewChildren, strict = true),
        )
    }
}
