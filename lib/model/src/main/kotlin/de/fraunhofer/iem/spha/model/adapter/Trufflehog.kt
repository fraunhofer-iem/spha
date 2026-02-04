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

@Serializable
data class TrufflehogResultDto(
    @SerialName("verified_secrets") val verifiedSecrets: Int?,
    @SerialName("unverified_secrets") val unverifiedSecrets: Int?,
    @SerialName("origins") val origins: List<TrufflehogFindingDto> = listOf(),
) : ToolResult

/**
 * Represents a single finding from TruffleHog's NDJSON output format. Each line in the NDJSON
 * output is a separate finding with this structure.
 */
@Serializable
data class TrufflehogFindingDto(
    @SerialName("SourceMetadata") val sourceMetadata: TrufflehogSourceMetadata? = null,
    @SerialName("SourceID") val sourceId: Int? = null,
    @SerialName("SourceType") val sourceType: Int? = null,
    @SerialName("SourceName") val sourceName: String? = null,
    @SerialName("DetectorType") val detectorType: Int? = null,
    @SerialName("DetectorName") val detectorName: String? = null,
    @SerialName("DetectorDescription") val detectorDescription: String? = null,
    @SerialName("DecoderName") val decoderName: String? = null,
    @SerialName("Verified") val verified: Boolean,
    @SerialName("VerificationFromCache") val verificationFromCache: Boolean? = null,
    @SerialName("Raw") val raw: String? = null,
    @SerialName("RawV2") val rawV2: String? = null,
    @SerialName("Redacted") val redacted: String? = null,
) : Origin

@Serializable
data class TrufflehogSourceMetadata(
    @SerialName("Data") val data: TrufflehogSourceData? = null,
)

@Serializable
data class TrufflehogSourceData(
    @SerialName("Github") val github: TrufflehogGithubData? = null,
)

@Serializable
data class TrufflehogGithubData(
    val link: String? = null,
    val repository: String? = null,
    val commit: String? = null,
    val email: String? = null,
    val file: String? = null,
    val timestamp: String? = null,
    val line: Int? = null,
)
