/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi.hierarchy

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import java.util.UUID
import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class KpiResultHierarchy
private constructor(val rootNode: KpiResultNode, val schemaVersion: String) {
    companion object {
        fun create(rootNode: KpiResultNode) = KpiResultHierarchy(rootNode, SCHEMA_VERSIONS.last())
    }
}

@Serializable
data class KpiResultNode(
    val typeId: String,
    val result: KpiCalculationResult,
    val strategy: KpiStrategyId,
    val children: List<KpiResultEdge>,
    val id: String = UUID.randomUUID().toString(),
    val originId: String? = null,
    val reason: String? = null
)

@Serializable
data class KpiResultEdge(
    val target: KpiResultNode,
    val plannedWeight: Double,
    val actualWeight: Double,
)

@Serializable
sealed class KpiCalculationResult {
    @Serializable data class Success(val score: Int) : KpiCalculationResult()

    @Serializable data class Error(val reason: String) : KpiCalculationResult()

    @Serializable
    data class Incomplete(val score: Int, val reason: String) : KpiCalculationResult()

    @Serializable
    data class Empty(val reason: String = "This KPI is empty") : KpiCalculationResult()
}
