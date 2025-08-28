/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import de.fraunhofer.iem.spha.model.adapter.Origin
import de.fraunhofer.iem.spha.model.adapter.ToolResult
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

enum class ErrorType {
    DATA_VALIDATION_ERROR
}

abstract class KpiAdapter<T : ToolResult, O : Origin> {

    protected val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    protected val logger = KotlinLogging.logger {}

    abstract fun transformDataToKpi(vararg data: T): AdapterResult<O>

    @OptIn(ExperimentalSerializationApi::class)
    fun dtoFromJson(jsonData: InputStream, deserializer: KSerializer<T>): T {
        return jsonParser.decodeFromStream(deserializer, jsonData)
    }
}

@Serializable
data class ToolInfo(val name: String, val description: String, val version: String? = null)

@Serializable
data class AdapterResult<T : Origin>(
    val toolInfo: ToolInfo? = null,
    val transformationResults: Collection<TransformationResult<T>> = emptyList(),
)

@Serializable
sealed class TransformationResult<out T : Origin> {
    /**
     * @param origin describes the data that the RawValueKpi was created from. If, for some reason,
     *   no origin data is available, T should be set to Unit.
     */
    sealed class Success<T : Origin>(val rawValueKpi: RawValueKpi, val origin: T) :
        TransformationResult<T>() {
        class Kpi<T : Origin>(rawValueKpi: RawValueKpi, origin: T) :
            Success<T>(rawValueKpi, origin)

        override fun toString(): String {
            return "[Adapter Result Success]: $rawValueKpi"
        }
    }

    data class Error(val type: ErrorType) : TransformationResult<Nothing>()
}
