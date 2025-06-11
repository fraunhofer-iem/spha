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

class XorKPICalculationStrategyTest {

    @Test
    fun emptyEdges() {
        assertEquals(
            KpiCalculationResult.Empty(),
            XorKPICalculationStrategy.calculateKpi(listOf(), strict = true),
        )
        assertEquals(
            KpiCalculationResult.Empty(),
            XorKPICalculationStrategy.calculateKpi(listOf(), strict = false),
        )
    }

    @Test
    fun tooManyEdges() {
        val nodeManyChildren =
            KpiNode(
                typeId = KpiType.ROOT.name,
                strategy = KpiStrategyId.XOR_STRATEGY,
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
            false,
            XorKPICalculationStrategy.isValid(node = nodeManyChildren, strict = false),
        )
        assertEquals(
            false,
            XorKPICalculationStrategy.isValid(node = nodeManyChildren, strict = true),
        )

        val root =
            KpiHierarchyNode.from(
                nodeManyChildren,
                listOf(RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 15)),
            )

        val relaxed = XorKPICalculationStrategy.calculateKpi(root.hierarchyEdges, strict = false)
        val strict = XorKPICalculationStrategy.calculateKpi(root.hierarchyEdges, strict = true)
        assertEquals(true, relaxed is KpiCalculationResult.Error)
        assertEquals(true, strict is KpiCalculationResult.Error)
    }

    @Test
    fun calculateValidHierarchyFalseResult() {
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
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = 50),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 20),
                ),
            )

        val calcRelaxed =
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = false,
            )

        val calcStrict =
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = true,
            )

        assertEquals(true, calcStrict is KpiCalculationResult.Success)
        assertEquals(true, calcRelaxed is KpiCalculationResult.Success)

        assertEquals(0, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(0, (calcRelaxed as KpiCalculationResult.Success).score)
    }

    @Test
    fun calculateCorrectEqual() {
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
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = false,
            )

        val calcStrict =
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = true,
            )

        assertEquals(true, calcStrict is KpiCalculationResult.Success)
        assertEquals(true, calcRelaxed is KpiCalculationResult.Success)

        assertEquals(0, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(0, (calcRelaxed as KpiCalculationResult.Success).score)

        val rootZero =
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
                    RawValueKpi(typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name, score = 0),
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 0),
                ),
            )

        val calcRelaxedZero =
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = rootZero.hierarchyEdges,
                strict = false,
            )

        val calcStrictZero =
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = rootZero.hierarchyEdges,
                strict = true,
            )

        assertEquals(true, calcRelaxedZero is KpiCalculationResult.Success)
        assertEquals(true, calcStrictZero is KpiCalculationResult.Success)

        assertEquals(0, (calcRelaxedZero as KpiCalculationResult.Success).score)
        assertEquals(0, (calcStrictZero as KpiCalculationResult.Success).score)
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
                    RawValueKpi(typeId = KpiType.NUMBER_OF_COMMITS.name, score = 20),
                ),
            )

        val calcRelaxed =
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = false,
            )

        val calcStrict =
            XorKPICalculationStrategy.calculateKpi(
                hierarchyEdges = root.hierarchyEdges,
                strict = true,
            )

        assertEquals(true, calcStrict is KpiCalculationResult.Success)
        assertEquals(true, calcRelaxed is KpiCalculationResult.Success)

        assertEquals(100, (calcStrict as KpiCalculationResult.Success).score)
        assertEquals(100, (calcRelaxed as KpiCalculationResult.Success).score)
    }
}
