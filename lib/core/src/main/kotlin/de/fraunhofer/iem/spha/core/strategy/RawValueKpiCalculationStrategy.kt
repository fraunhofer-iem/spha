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
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

internal object RawValueKpiCalculationStrategy : BaseKpiCalculationStrategy() {
    override val kpiStrategyId: KpiStrategyId
        get() = KpiStrategyId.RAW_VALUE_STRATEGY

    override fun internalCalculateKpi(edges: Collection<KpiHierarchyEdge>): KpiCalculationResult {
        return KpiCalculationResult.Empty()
    }

    override fun internalIsValid(node: KpiNode, strict: Boolean): Boolean {
        return if (node.edges.isNotEmpty()) {
            logger.error {
                "Raw Value nodes must not have children as they expect only tool results."
            }
            false
        } else {
            true
        }
    }
}
