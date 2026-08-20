/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CycloneDXDto(
    @SerialName("bomFormat") val bomFormat: String? = null,
    @SerialName("specVersion") val specVersion: String? = null,
    @SerialName("vulnerabilities") val vulnerabilities: List<CycloneDXVulnerabilityDto> = listOf(),
) : ToolResult

@Serializable
data class CycloneDXVulnerabilityDto(
    @SerialName("id") val id: String,
    @SerialName("ratings") val ratings: List<CycloneDXRating> = listOf(),
    @SerialName("affects") val affects: List<CycloneDXAffects> = listOf(),
) : Origin

@Serializable
data class CycloneDXRating(
    @SerialName("score") val score: Double? = null,
    @SerialName("severity") val severity: String? = null,
    // e.g. "CVSSv2", "CVSSv3", "CVSSv31", "CVSSv4"
    @SerialName("method") val method: String? = null,
    @SerialName("vector") val vector: String? = null,
)

@Serializable data class CycloneDXAffects(@SerialName("ref") val ref: String? = null)
