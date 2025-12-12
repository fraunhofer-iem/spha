/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.reporter

import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode

fun KpiResultNode.getScoreVisualization(): String {
    return "${this.metaInfo?.displayName}:" +
        when (this.result) {
            is KpiCalculationResult.Empty -> (this.result as KpiCalculationResult.Empty).reason
            is KpiCalculationResult.Error -> (this.result as KpiCalculationResult.Error).reason
            is KpiCalculationResult.Incomplete ->
                "${(this.result as KpiCalculationResult.Incomplete).score} / 100"
            is KpiCalculationResult.Success ->
                "${(this.result as KpiCalculationResult.Success).score} / 100"
        }
}

fun KpiResultHierarchy.getMarkdown(): String {

    val topLevelScore = this.root.getScoreVisualization()

    val firstLevelScores =
        this.root.edges
            .map { edge -> edge.target.getScoreVisualization() }
            .reduce { a, b ->
                """$a   
                    | $b"""
                    .trimMargin()
            }

    return """
           |# $topLevelScore 
           | ## Top level KPI Scores
           | $firstLevelScores"""
        .trimMargin()
}
