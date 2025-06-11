/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.hierarchy

import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode
import java.util.*

internal class KpiHierarchyNode
private constructor(
    val typeId: String,
    val strategy: KpiStrategyId,
    val hierarchyEdges: List<KpiHierarchyEdge>,
    val id: String = UUID.randomUUID().toString(),
    val originId: String? = null,
) {

    var result: KpiCalculationResult = KpiCalculationResult.Empty()

    val score: Int
        get() =
            (result as? KpiCalculationResult.Success)?.score
                ?: (result as? KpiCalculationResult.Incomplete)?.score
                ?: 0

    fun hasNoResult(): Boolean {
        return (result is KpiCalculationResult.Empty) || (result is KpiCalculationResult.Error)
    }

    fun hasIncompleteResult(): Boolean {
        return result is KpiCalculationResult.Incomplete
    }

    override fun toString(): String {
        return "KpiHierarchyNode($typeId, $strategy, $result, $hierarchyEdges)"
    }

    companion object {
        fun to(node: KpiHierarchyNode): KpiResultNode {
            return KpiResultNode(
                typeId = node.typeId,
                strategy = node.strategy,
                result = node.result,
                originId = node.originId,
                children =
                    node.hierarchyEdges.map {
                        KpiResultEdge(
                            target = to(it.to),
                            plannedWeight = it.plannedWeight,
                            actualWeight = it.actualWeight,
                        )
                    },
            )
        }

        fun from(node: KpiNode, rawValueKpis: List<RawValueKpi>): KpiHierarchyNode {
            val kpiIdToValues = mutableMapOf<String, MutableList<RawValueKpi>>()

            rawValueKpis.forEach {
                if (!kpiIdToValues.containsKey(it.typeId)) {
                    kpiIdToValues[it.typeId] = mutableListOf()
                }
                kpiIdToValues[it.typeId]!!.add(it)
            }

            val hierarchy = from(node, kpiIdToValues = kpiIdToValues)

            return hierarchy
        }

        private fun from(
            node: KpiNode,
            kpiIdToValues: Map<String, List<RawValueKpi>>,
        ): KpiHierarchyNode {

            val children: MutableList<KpiHierarchyEdge> = mutableListOf()
            node.edges.forEach { child ->
                val rawValues = kpiIdToValues[child.target.typeId] ?: emptyList()
                if (rawValues.isNotEmpty()) {
                    rawValues.forEach { rawValueKpi ->
                        val hierarchyNode =
                            KpiHierarchyNode(
                                typeId = child.target.typeId,
                                // we force the kpi strategy to be raw value if we had a
                                // raw value for the given node.
                                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                hierarchyEdges = emptyList(),
                                originId = rawValueKpi.originId,
                                id = rawValueKpi.id,
                            )
                        hierarchyNode.result = KpiCalculationResult.Success(rawValueKpi.score)
                        val edge =
                            KpiHierarchyEdge(
                                to = hierarchyNode,
                                plannedWeight = child.weight / rawValues.count(),
                            )
                        children.add(edge)
                    }
                } else {
                    children.add(
                        KpiHierarchyEdge(
                            to = from(child.target, kpiIdToValues),
                            plannedWeight = child.weight,
                        )
                    )
                }
            }

            val calcNode =
                KpiHierarchyNode(
                    typeId = node.typeId,
                    hierarchyEdges = children,
                    strategy = node.strategy,
                )

            return calcNode
        }

        fun depthFirstTraversal(
            node: KpiHierarchyNode,
            seen: MutableSet<KpiHierarchyNode> = mutableSetOf(),
            action: (node: KpiHierarchyNode) -> Unit,
        ) {
            if (!seen.add(node)) {
                return
            }

            node.hierarchyEdges.forEach { child ->
                depthFirstTraversal(node = child.to, seen = seen, action)
            }

            action(node)
        }
    }
}
