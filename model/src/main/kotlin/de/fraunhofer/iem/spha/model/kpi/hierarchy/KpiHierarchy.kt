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
import kotlinx.serialization.Serializable

val SCHEMA_VERSIONS: Array<String> = arrayOf("1.1.0").sortedArray()

// XXX: add Hierarchy Validator
@ConsistentCopyVisibility
@Serializable
data class KpiHierarchy private constructor(val root: KpiNode, val schemaVersion: String) {
    companion object {
        fun create(root: KpiNode) = KpiHierarchy(root, SCHEMA_VERSIONS.last())
    }
}

@Serializable
data class KpiNode(
    val typeId: String,
    val strategy: KpiStrategyId,
    val edges: List<KpiEdge>,
    val tags: Set<String> = emptySet(),
    val reason: String? = null,
    val thresholds: List<Threshold> = emptyList(),
)

@Serializable data class KpiEdge(val target: KpiNode, val weight: Double)
