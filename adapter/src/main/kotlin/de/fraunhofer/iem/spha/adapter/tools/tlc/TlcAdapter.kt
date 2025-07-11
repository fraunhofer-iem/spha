/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.Project
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.Version
import de.fraunhofer.iem.spha.adapter.tools.tlc.util.TechLagHelper.getTechLagForGraph
import de.fraunhofer.iem.spha.model.adapter.TlcConfig
import de.fraunhofer.iem.spha.model.adapter.TlcDefaultConfig
import de.fraunhofer.iem.spha.model.adapter.TlcDto
import de.fraunhofer.iem.spha.model.adapter.TlcOriginDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

sealed class TechLagResult {
    data class Success(val libyear: Long) : TechLagResult()

    data class Empty(val reason: String) : TechLagResult()
}

object TlcAdapter : KpiAdapter<TlcDto, TlcOriginDto>() {

    var config: TlcConfig = TlcDefaultConfig.get()

    override fun transformDataToKpi(vararg data: TlcDto): Collection<AdapterResult<TlcOriginDto>> {

        return data.flatMap { tlcDto ->
            tlcDto.projectDtos.flatMap { projectDto ->
                val project = Project.from(projectDto)
                project.graph.map { (scope, graph) ->
                    val techLag =
                        getTechLagForGraph(
                            graph = graph,
                            artifacts = project.artifacts,
                            targetVersion = Version.Major,
                        )

                    if (techLag is TechLagResult.Success) {

                        val libyearScore = getLibyearScore(techLag.libyear, config)

                        val rawValueKpi =
                            if (isProductionScope(ecosystem = project.ecosystem, scope = scope)) {
                                RawValueKpi(
                                    score = libyearScore,
                                    typeId = KpiType.LIB_DAYS_PROD.name,
                                )
                            } else {
                                RawValueKpi(
                                    score = libyearScore,
                                    typeId = KpiType.LIB_DAYS_DEV.name,
                                )
                            }

                        return@map AdapterResult.Success.Kpi(
                            rawValueKpi = rawValueKpi,
                            origin = TlcOriginDto(projectDto, techLag.libyear),
                        )
                    }

                    return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
                }
            }
        }
    }

    private fun getLibyearScore(libyear: Long, config: TlcConfig): Int {

        if (libyear < 0L) {
            return 100
        }

        val sortedThresholds = config.thresholds.sortedBy { it.range.from }

        sortedThresholds.forEach { threshold ->
            if (libyear > threshold.range.from && libyear < threshold.range.to) {
                return threshold.score
            }
        }

        return 0
    }

    private fun isProductionScope(scope: String, ecosystem: String): Boolean {
        return when (ecosystem) {
            "NPM" -> "dependencies" == scope
            else -> true
        }
    }
}
