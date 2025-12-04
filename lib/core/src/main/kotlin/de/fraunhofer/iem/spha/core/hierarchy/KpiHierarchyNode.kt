/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
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
import de.fraunhofer.iem.spha.model.kpi.hierarchy.MetaInfo
import de.fraunhofer.iem.spha.model.kpi.hierarchy.Threshold
import java.util.UUID

internal class KpiHierarchyNode
private constructor(
    val typeId: String,
    val strategy: KpiStrategyId,
    val edges: List<KpiHierarchyEdge>,
    val thresholds: List<Threshold> = emptyList(),
    val originId: String? = null,
    val metaInfo: MetaInfo? = null,
) {
    private var _id: String = UUID.randomUUID().toString()
    val id: String
        get() = _id

    constructor(
        typeId: String,
        strategy: KpiStrategyId,
        edges: List<KpiHierarchyEdge>,
        id: String,
        thresholds: List<Threshold> = emptyList(),
        originId: String? = null,
        metaInfo: MetaInfo? = null,
    ) : this(typeId, strategy, edges, thresholds, originId, metaInfo) {
        this._id = id
    }

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
        return "KpiHierarchyNode($typeId, $strategy, $result, $edges)"
    }

    companion object {
        fun to(node: KpiHierarchyNode): KpiResultNode {
            return KpiResultNode(
                typeId = node.typeId,
                strategy = node.strategy,
                result = node.result,
                originId = node.originId,
                id = node.id,
                metaInfo = node.metaInfo,
                thresholds = node.thresholds,
                edges =
                    node.edges.map {
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

            val hierarchy = from(node, typeIdToRawValue = kpiIdToValues)

            return hierarchy
        }

        private fun from(
            node: KpiNode,
            typeIdToRawValue: Map<String, List<RawValueKpi>>,
        ): KpiHierarchyNode {

            val edges: MutableList<KpiHierarchyEdge> = mutableListOf()
            node.edges.forEach { child ->
                val rawValues = typeIdToRawValue[child.target.typeId] ?: emptyList()
                if (rawValues.isNotEmpty()) {
                    rawValues.forEach { rawValueKpi ->
                        val hierarchyNode =
                            KpiHierarchyNode(
                                typeId = child.target.typeId,
                                // we force the kpi strategy to be raw value if we had a
                                // raw value for the given node.
                                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                                edges = emptyList(),
                                originId = rawValueKpi.originId,
                                id = rawValueKpi.id,
                                thresholds = child.target.thresholds,
                                metaInfo = child.target.metaInfo,
                            )
                        hierarchyNode.result = KpiCalculationResult.Success(rawValueKpi.score)
                        val edge =
                            KpiHierarchyEdge(
                                to = hierarchyNode,
                                plannedWeight = child.weight / rawValues.count(),
                            )
                        edges.add(edge)
                    }
                } else {
                    edges.add(
                        KpiHierarchyEdge(
                            to = from(child.target, typeIdToRawValue),
                            plannedWeight = child.weight,
                        )
                    )
                }
            }

            val calcNode =
                KpiHierarchyNode(
                    typeId = node.typeId,
                    edges = edges,
                    strategy = node.strategy,
                    thresholds = node.thresholds,
                    metaInfo = node.metaInfo,
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

            node.edges.forEach { child ->
                depthFirstTraversal(node = child.to, seen = seen, action)
            }

            action(node)
        }
    }
}
