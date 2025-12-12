/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.reporter

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class ReporterTest {
    private fun randomKpiResultNode(): KpiResultNode {
        return KpiResultNode(
            typeId = KpiType.SECURITY.name,
            result = KpiCalculationResult.Success(score = 65),
            strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
            edges = listOf(),
        )
    }

    private fun randomKpiResultEdge(): KpiResultEdge {
        return KpiResultEdge(
            target = randomKpiResultNode(),
            plannedWeight = 0.3,
            actualWeight = 0.3,
        )
    }

    @Test
    fun markdownForValidResultHierarchy() {
        val kpiResultHierarchy =
            KpiResultHierarchy.create(
                KpiResultNode(
                    typeId = KpiType.ROOT.name,
                    result = KpiCalculationResult.Success(score = 75),
                    strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                    edges =
                        listOf(
                            KpiResultEdge(
                                target =
                                    KpiResultNode(
                                        typeId = KpiType.SECURITY.name,
                                        result = KpiCalculationResult.Success(score = 65),
                                        strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                                        edges =
                                            listOf(randomKpiResultEdge(), randomKpiResultEdge()),
                                        id = "",
                                        originId = null,
                                    ),
                                plannedWeight = 0.4,
                                actualWeight = 0.4,
                            ),
                            KpiResultEdge(
                                target =
                                    KpiResultNode(
                                        typeId = KpiType.EXTERNAL_QUALITY.name,
                                        result = KpiCalculationResult.Success(score = 85),
                                        strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                                        edges = listOf(randomKpiResultEdge(), randomKpiResultEdge()),
                                    ),
                                plannedWeight = 0.4,
                                actualWeight = 0.4,
                            ),
                        ),
                    id = "",
                    originId = null,
                )
            )

        val markdown = kpiResultHierarchy.getMarkdown()
        assertNotNull(markdown)
        assertNotEquals("", markdown)
    }
}
