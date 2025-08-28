/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
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

internal object WeightedMaximumKPICalculationStrategy : BaseKpiCalculationStrategy() {

    override val kpiStrategyId: KpiStrategyId
        get() = KpiStrategyId.WEIGHTED_MAXIMUM_STRATEGY

    override fun internalCalculateKpi(edges: Collection<KpiHierarchyEdge>): KpiCalculationResult {

        val max = edges.maxByOrNull { it.to.score * it.actualWeight }?.to?.score

        if (max == null) {
            return KpiCalculationResult.Empty()
        }

        return KpiCalculationResult.Success(score = max)
    }

    /** There is no validity requirement for this strategy. */
    override fun internalIsValid(node: KpiNode, strict: Boolean): Boolean {

        if (node.edges.size == 1) {
            logger.warn {
                "Weighted maximum KPI calculation strategy for node $node is planned " +
                    "for a single child."
            }
        }

        return true
    }
}
