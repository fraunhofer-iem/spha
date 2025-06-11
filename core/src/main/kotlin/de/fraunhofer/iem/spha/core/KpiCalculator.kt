/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core

import de.fraunhofer.iem.spha.core.hierarchy.KpiHierarchyNode
import de.fraunhofer.iem.spha.core.hierarchy.KpiHierarchyNode.Companion.depthFirstTraversal
import de.fraunhofer.iem.spha.core.strategy.getKpiCalculationStrategy
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import io.github.oshai.kotlinlogging.KotlinLogging

object KpiCalculator {

    private val logger = KotlinLogging.logger {}

    fun calculateKpis(
        hierarchy: KpiHierarchy,
        rawValueKpis: List<RawValueKpi>,
        strict: Boolean = false,
    ): KpiResultHierarchy {
        logger.info {
            "Running KPI calculation on $hierarchy and $rawValueKpis with strict mode=$strict"
        }
        val root = KpiHierarchyNode.from(hierarchy.root, rawValueKpis)

        depthFirstTraversal(root) { it.result = calculateKpi(it, strict) }

        return KpiResultHierarchy.create(KpiHierarchyNode.to(root))
    }

    /** Selects and executes the Kpi strategy related to the given node */
    internal fun calculateKpi(
        node: KpiHierarchyNode,
        strict: Boolean = false,
    ): KpiCalculationResult {
        logger.info { "Running KPI calculation on $node" }
        if (node.strategy == KpiStrategyId.RAW_VALUE_STRATEGY) {
            return node.result
        }

        val result =
            getKpiCalculationStrategy(node.strategy).calculateKpi(node.hierarchyEdges, strict)
        logger.info { "KPI calculation result $result" }
        return result
    }
}
