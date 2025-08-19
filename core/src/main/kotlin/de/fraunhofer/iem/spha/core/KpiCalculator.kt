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
import de.fraunhofer.iem.spha.model.kpi.KpiType
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
            return transformRawValue(node)
        }

        val result = getKpiCalculationStrategy(node.strategy).calculateKpi(node.edges, strict)
        logger.info { "KPI calculation result $result" }
        return result
    }

    internal fun transformRawValue(node: KpiHierarchyNode): KpiCalculationResult {
        return when (node.typeId) {
            KpiType.TECHNICAL_LAG_DEV_DIRECT_COMPONENT.name -> transformTechLagComponent(node)

            else -> node.result
        }
    }

    internal fun transformTechLagComponent(node: KpiHierarchyNode): KpiCalculationResult {
        // get the highest threshold from the node
        val highestThreshold =
            node.thresholds.maxByOrNull { it.value }
                ?: return KpiCalculationResult.Error("Thresholds for node $node are empty.")

        if (node.score < 0) {
            return KpiCalculationResult.Error("Score for node $node is negative.")
        }

        val score =
            if (node.score <= highestThreshold.value) {
                100
            } else if (node.score > highestThreshold.value * 2) {
                0
            } else {
                (1 - ((node.score - highestThreshold.value) / (highestThreshold.value * 2)))*100
            }

        return KpiCalculationResult.Success(score)
    }
}
