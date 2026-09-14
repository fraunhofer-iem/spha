/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class CycloneDXDto(
    @SerialName("bomFormat") val bomFormat: String,
    @SerialName("specVersion") val specVersion: String,
    @SerialName("vulnerabilities") val vulnerabilities: List<CycloneDXVulnerabilityDto>? = null,
) : ToolResult

@Serializable
data class CycloneDXVulnerabilityDto(
    @SerialName("id") val id: String = "Unknown",
    @SerialName("ratings") val ratings: List<CycloneDXRating> = listOf(),
    @SerialName("affects") val affects: List<CycloneDXAffects> = listOf(),
) : Origin

@Serializable
enum class CycloneDXSeverity(val score: Double?) {
    @SerialName("critical") CRITICAL(9.5), // 9.0 - 10.0
    @SerialName("high") HIGH(8.0), // 7.0 - 8.9
    @SerialName("medium") MEDIUM(5.5), // 4.0 - 6.9
    @SerialName("low") LOW(2.0), // 0.1 - 3.9
    @SerialName("info") INFO(0.0),
    @SerialName("none") NONE(0.0),
    @SerialName("unknown") UNKNOWN(null),
}

@OptIn(ExperimentalSerializationApi::class)
object CycloneDXSeverityNullableSerializer : KSerializer<CycloneDXSeverity?> {

    private val delegate = CycloneDXSeverity.serializer()

    override val descriptor: SerialDescriptor =
        SerialDescriptor("CycloneDXSeverityOrNull", delegate.descriptor).nullable

    override fun deserialize(decoder: Decoder): CycloneDXSeverity? =
        try {
            delegate.deserialize(decoder)
        } catch (_: SerializationException) {
            null
        }

    override fun serialize(encoder: Encoder, value: CycloneDXSeverity?) {
        if (value == null) encoder.encodeNull() else delegate.serialize(encoder, value)
    }
}

@Serializable
data class CycloneDXRating(
    @SerialName("score") val score: Double? = null,
    @SerialName("severity")
    @Serializable(with = CycloneDXSeverityNullableSerializer::class)
    val severity: CycloneDXSeverity? = null,
    @SerialName("method") val method: String? = null,
    @SerialName("vector") val vector: String? = null,
)

@Serializable data class CycloneDXAffects(@SerialName("ref") val ref: String? = null)
