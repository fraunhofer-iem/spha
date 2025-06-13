/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trufflehog

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.model.adapter.trufflehog.TrufflehogDto
import de.fraunhofer.iem.spha.model.adapter.trufflehog.TrufflehogReportDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream

object TrufflehogAdapter {
    private val logger = KotlinLogging.logger {}
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dtoFromJson(jsonData: InputStream): List<TrufflehogReportDto> {
        val rawResult = jsonParser.decodeFromStream<TrufflehogDto>(jsonData)
        return rawResult.results.mapNotNull {
            try {
                jsonParser.decodeFromJsonElement<TrufflehogReportDto>(it)
            } catch (e: Exception) {
                logger.warn { "Decoding of trufflehog result failed for $it with ${e.message}" }
                null
            }
        }
    }

    fun transformDataToKpi(
        data: TrufflehogReportDto
    ): Collection<AdapterResult<TrufflehogReportDto>> {
        return transformDataToKpi(listOf(data))
    }

    fun transformDataToKpi(
        data: Collection<TrufflehogReportDto>
    ): Collection<AdapterResult<TrufflehogReportDto>> {
        return data.map {
            val score = if (it.verifiedSecrets > 0) 0 else 100
            AdapterResult.Success.Kpi(
                RawValueKpi(score = score, typeId = KpiType.SECRETS.name),
                origin = it,
            )
        }
    }
}
