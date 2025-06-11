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
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode

internal fun randomKpiHierarchyNode(): KpiHierarchyNode {

    val rndIds = (0..<KpiType.entries.size).random()
    val rndStrategies = (0..<KpiStrategyId.entries.size).random()

    return KpiHierarchyNode.from(
        KpiNode(
            kpiId = KpiType.entries[rndIds].name,
            kpiStrategyId = KpiStrategyId.entries[rndStrategies],
            edges = listOf(),
        ),
        emptyList(),
    )
}

fun randomNode(edges: List<KpiEdge> = listOf()): KpiNode {

    val rndIds = (0..<KpiType.entries.size).random()
    val rndStrategies = (0..<KpiStrategyId.entries.size).random()

    return KpiNode(
        kpiId = KpiType.entries[rndIds].name,
        kpiStrategyId = KpiStrategyId.entries[rndStrategies],
        edges = edges,
    )
}
