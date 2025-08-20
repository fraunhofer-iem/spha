/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.transformation

import de.fraunhofer.iem.spha.core.hierarchy.KpiHierarchyNode
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult

/** Interface for transforming raw KPI values based on node type */
internal interface RawValueTransformer {
    /**
     * Transforms a raw value node to a calculated result
     *
     * @param node The KPI hierarchy node to transform
     * @return The calculated KPI result
     */
    fun transform(node: KpiHierarchyNode): KpiCalculationResult
}

/**
 * Default implementation of RawValueTransformer that delegates to specific transformers based on a
 * node type
 */
internal object DefaultRawValueTransformer : RawValueTransformer {

    override fun transform(node: KpiHierarchyNode): KpiCalculationResult {
        return when (node.typeId) {
            KpiType.TECHNICAL_LAG_DEV_DIRECT_COMPONENT.name -> transformTechLagComponent(node)
            else -> node.result
        }
    }

    /** Transforms a technical lag component node by calculating a score based on thresholds */
    internal fun transformTechLagComponent(node: KpiHierarchyNode): KpiCalculationResult {
        // get the highest threshold from the node
        val highestThreshold =
            node.thresholds.maxByOrNull { it.value }
                ?: return KpiCalculationResult.Error("Thresholds for node $node are empty.")

        if (node.score < 0) {
            return KpiCalculationResult.Error("Score for node $node is negative.")
        }

        fun calculateTechLagScore(score: Int, threshold: Int): Int {
            return when {
                score <= threshold -> 100
                score > threshold * 2 -> 0
                else -> {
                    val ratio = (score - threshold).toDouble() / threshold
                    ((1.0 - ratio) * 100).toInt()
                }
            }
        }

        // Then in your main function:
        val score = calculateTechLagScore(node.score, highestThreshold.value)

        return KpiCalculationResult.Success(score)
    }
}
