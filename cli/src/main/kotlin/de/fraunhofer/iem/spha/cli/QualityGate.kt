/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli

import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode

/** Verdict for a single required gate node. */
internal data class GateNodeVerdict(val typeId: String, val passed: Boolean, val detail: String)

/** Aggregate outcome of a gate evaluation. */
internal data class GateEvaluation(val passed: Boolean, val verdicts: List<GateNodeVerdict>)

/**
 * Fail-closed quality-gate evaluation over a computed [KpiResultHierarchy].
 *
 * A required node passes ONLY if its result is [KpiCalculationResult.Success] with a score at or
 * above `minScore`. Everything else fails the gate: [KpiCalculationResult.Error],
 * [KpiCalculationResult.Incomplete], [KpiCalculationResult.Empty] (e.g. missing raw data for a
 * leaf), a score below the threshold, or a required node that is absent from the hierarchy.
 *
 * This is the typed, in-tool equivalent of the external `gate.sh` wrapper: it inspects the
 * [KpiCalculationResult] variants directly, so it is immune to serialization details such as the
 * fully-qualified sealed-class discriminator name in the output JSON.
 */
internal object QualityGate {

    /**
     * @param requiredNodeTypeIds typeIds that must pass. Empty defaults to the hierarchy root.
     * @param minScore the minimum passing score for each required node.
     */
    fun evaluate(
        hierarchy: KpiResultHierarchy,
        requiredNodeTypeIds: List<String>,
        minScore: Int,
    ): GateEvaluation {
        val required = requiredNodeTypeIds.ifEmpty { listOf(hierarchy.root.typeId) }
        val nodesByTypeId = collectNodes(hierarchy.root).groupBy { it.typeId }

        val verdicts =
            required.map { typeId ->
                val matches = nodesByTypeId[typeId].orEmpty()
                if (matches.isEmpty()) {
                    GateNodeVerdict(typeId, false, "required node is absent from the hierarchy")
                } else {
                    // Every node sharing this typeId must pass; report the first failure, if any.
                    val evaluated = matches.map { evaluateResult(it.result, minScore) }
                    val firstFailure = evaluated.firstOrNull { !it.first }
                    val chosen = firstFailure ?: evaluated.first()
                    GateNodeVerdict(typeId, chosen.first, chosen.second)
                }
            }

        return GateEvaluation(verdicts.all { it.passed }, verdicts)
    }

    private fun evaluateResult(result: KpiCalculationResult, minScore: Int): Pair<Boolean, String> =
        when (result) {
            is KpiCalculationResult.Success ->
                if (result.score >= minScore) true to "Success@${result.score} (>= $minScore)"
                else false to "Success@${result.score} below required min-score $minScore"
            is KpiCalculationResult.Incomplete ->
                false to "Incomplete@${result.score}: ${result.reason}"
            is KpiCalculationResult.Error -> false to "Error: ${result.reason}"
            is KpiCalculationResult.Empty -> false to "Empty: ${result.reason}"
        }

    private fun collectNodes(root: KpiResultNode): List<KpiResultNode> {
        val nodes = mutableListOf<KpiResultNode>()
        val stack = ArrayDeque<KpiResultNode>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            nodes.add(node)
            node.edges.forEach { stack.addLast(it.target) }
        }
        return nodes
    }
}
