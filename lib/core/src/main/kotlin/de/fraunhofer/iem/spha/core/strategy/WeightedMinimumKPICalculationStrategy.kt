/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.strategy

import de.fraunhofer.iem.spha.core.hierarchy.KpiHierarchyEdge
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

internal object WeightedMinimumKPICalculationStrategy : BaseKpiCalculationStrategy() {

    override val kpiStrategyId: KpiStrategyId
        get() = KpiStrategyId.WEIGHTED_MINIMUM_STRATEGY

    override fun internalCalculateKpi(edges: Collection<KpiHierarchyEdge>): KpiCalculationResult {

        val min = edges.minByOrNull { it.to.score * it.actualWeight }?.to?.score

        if (min == null) {
            return KpiCalculationResult.Empty()
        }

        return KpiCalculationResult.Success(score = min)
    }

    /** There is no validity requirement for this strategy. */
    override fun internalIsValid(node: KpiNode, strict: Boolean): Boolean {

        if (node.edges.size == 1) {
            logger.warn {
                "Weighted minimum KPI calculation strategy for node $node is planned " +
                    "for a single child."
            }
        }

        return true
    }
}
