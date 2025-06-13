/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.Project
import de.fraunhofer.iem.spha.adapter.tools.tlc.model.Version
import de.fraunhofer.iem.spha.adapter.tools.tlc.util.TechLagHelper.getTechLagForGraph
import de.fraunhofer.iem.spha.model.adapter.tlc.TlcConfig
import de.fraunhofer.iem.spha.model.adapter.tlc.TlcDefaultConfig
import de.fraunhofer.iem.spha.model.adapter.tlc.TlcDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

sealed class TechLagResult {
    data class Success(val libyear: Long) : TechLagResult()

    data class Empty(val reason: String) : TechLagResult()
}

object TlcAdapter {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dtoFromJson(jsonData: InputStream): TlcDto {
        return jsonParser.decodeFromStream<TlcDto>(jsonData)
    }

    fun transformDataToKpi(
        data: Collection<TlcDto>,
        config: TlcConfig = TlcDefaultConfig.get(),
    ): Collection<AdapterResult<TechLagResult>> {

        return data.flatMap { tlcDto ->
            tlcDto.projectDtos.flatMap {
                val project = Project.from(it)
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
                            origin = techLag,
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
