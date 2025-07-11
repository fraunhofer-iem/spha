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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class KpiResultHierarchy
private constructor(val root: KpiResultNode, val schemaVersion: String) {
    @OptIn(ExperimentalTime::class) val timestamp: String = Clock.System.now().toString()

    companion object {
        fun create(rootNode: KpiResultNode) = KpiResultHierarchy(rootNode, SCHEMA_VERSIONS.last())
    }
}

@Serializable
data class KpiResultNode(
    val typeId: String,
    val displayName: String,
    val result: KpiCalculationResult,
    val strategy: KpiStrategyId,
    val edges: List<KpiResultEdge>,
    val tags: Set<String> = emptySet(),
    val originId: String? = null,
    val reason: String? = null,
    val thresholds: List<Threshold> = emptyList(),
) {
    @SerialName("id") private var _id: String = UUID.randomUUID().toString()
    val id: String
        get() = _id

    constructor(
        typeId: String,
        result: KpiCalculationResult,
        displayName: String,
        strategy: KpiStrategyId,
        edges: List<KpiResultEdge>,
        id: String,
        tags: Set<String> = emptySet(),
        originId: String? = null,
        reason: String? = null,
        thresholds: List<Threshold> = emptyList(),
    ) : this(
        typeId = typeId,
        displayName = displayName,
        result = result,
        strategy = strategy,
        edges = edges,
        tags = tags,
        originId = originId,
        reason = reason,
        thresholds = thresholds,
    ) {
        this._id = id
    }
}

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
