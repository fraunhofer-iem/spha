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
data class TrufflehogFindingDto(
    @SerialName("origins") val origins: List<TrufflehogResultDto> = listOf(),
) : Origin

/** Represents a single finding from TruffleHog Note: SourceMetadata is skipped */
@Serializable
data class TrufflehogResultDto(
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
) : ToolResult
