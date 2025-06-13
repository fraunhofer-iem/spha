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

internal object WeightedRatioKPICalculationStrategy : BaseKpiCalculationStrategy() {

    override val kpiStrategyId: KpiStrategyId
        get() = KpiStrategyId.WEIGHTED_RATIO_STRATEGY

    /** Returns smallerValue / biggerValue, regardless in which order the values are given. */
    override fun internalCalculateKpi(edges: Collection<KpiHierarchyEdge>): KpiCalculationResult {

        if (edges.size != 2) {
            return KpiCalculationResult.Error(
                "Ratio calculation strategy called " +
                    "with ${edges.size} valid elements, which is illegal."
            )
        }

        if (edges.any { it.to.hasNoResult() }) {
            return KpiCalculationResult.Error(
                reason = "Ratio calculation strategy has elements without result"
            )
        }

        val biggerValue: Double
        val smallerValue: Double

        val firstScore = edges.first().to.score * edges.first().actualWeight
        val secondScore = edges.last().to.score * edges.last().actualWeight

        if (firstScore > secondScore) {
            biggerValue = firstScore
            smallerValue = secondScore
        } else {
            biggerValue = secondScore
            smallerValue = firstScore
        }

        val ratio =
            try {
                if (biggerValue != 0.0) {
                    smallerValue / biggerValue
                } else {
                    // the statement above results in minus infinity
                    // instead of an ArithmeticException when dividing by 0.

                    return KpiCalculationResult.Error("Tried division by 0")
                }
            } catch (e: Exception) {
                logger.error { "Error " }
                return KpiCalculationResult.Error(e.message ?: e.toString())
            }
        return KpiCalculationResult.Success(score = (ratio * 100).toInt())
    }

    /**
     * Validates whether the given KPI node is a valid ratio calculation node. If the given node's
     * strategy is not RATIO_STRATEGY, we return true.
     *
     * If the number of children is not two, a warning is generated, regardless of the used mode.
     *
     * @param node KPI node to validate.
     * @param strict validation mode, true implies that a valid node must contain exactly two
     *   children. False implies that a valid node must contain two or more children.
     * @return if the given node is valid.
     */
    override fun internalIsValid(node: KpiNode, strict: Boolean): Boolean {

        if (strict) {
            return node.edges.size == 2
        }

        if (node.edges.size > 2) {
            logger.warn {
                "Ratio KPI calculation strategy for node $node has more than two children." +
                    "This is allowed in relaxed mode, however it is not defined, which" +
                    "children are selected for calculation. This can lead to" +
                    "ambiguous results."
            }

            return true
        }

        return node.edges.size > 1
    }
}
