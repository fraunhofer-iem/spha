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
    val result: KpiCalculationResult,
    val strategy: KpiStrategyId,
    val edges: List<KpiResultEdge> = emptyList(),
    val thresholds: List<Threshold> = emptyList(),
    val originId: String? = null,
    val metaInfo: MetaInfo? = null,
) {
    @SerialName("id") private var _id: String = UUID.randomUUID().toString()
    val id: String
        get() = _id

    constructor(
        typeId: String,
        result: KpiCalculationResult,
        strategy: KpiStrategyId,
        edges: List<KpiResultEdge>,
        id: String,
        originId: String? = null,
        metaInfo: MetaInfo? = null,
        thresholds: List<Threshold> = emptyList(),
    ) : this(
        typeId = typeId,
        result = result,
        strategy = strategy,
        edges = edges,
        originId = originId,
        thresholds = thresholds,
        metaInfo = metaInfo,
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
