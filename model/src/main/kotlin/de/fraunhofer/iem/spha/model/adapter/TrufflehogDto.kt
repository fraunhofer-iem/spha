/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class TrufflehogDto(val results: List<JsonObject>)

@Serializable
data class TrufflehogReportDto(
    val level: String?,
    val ts: String?,
    val logger: String?,
    val msg: String?,
    val chunks: Int?,
    val bytes: Int?,
    @SerialName("verified_secrets") val verifiedSecrets: Int,
    @SerialName("unverified_secrets") val unverifiedSecrets: Int,
    @SerialName("scan_duration") val scanDuration: String?,
    @SerialName("trufflehog_version") val trufflehogVersion: String?,
) : ToolResult
