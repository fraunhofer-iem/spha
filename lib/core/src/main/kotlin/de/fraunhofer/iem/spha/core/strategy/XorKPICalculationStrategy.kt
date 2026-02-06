/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
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

internal object XorKPICalculationStrategy : BaseKpiCalculationStrategy() {

    override val kpiStrategyId: KpiStrategyId
        get() = KpiStrategyId.XOR_STRATEGY

    override fun internalCalculateKpi(edges: Collection<KpiHierarchyEdge>): KpiCalculationResult {

        if (edges.size != 2) {
            return KpiCalculationResult.Error(
                "XOR calculation strategy called " +
                    "with ${edges.size} valid elements, which is illegal."
            )
        }

        if ((edges.first().to.score == 100) != (edges.last().to.score == 100)) {
            return KpiCalculationResult.Success(score = 100)
        }

        return KpiCalculationResult.Success(score = 0)
    }

    override fun internalIsValid(node: KpiNode, strict: Boolean): Boolean {
        return node.edges.size == 2
    }
}
