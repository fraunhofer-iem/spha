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

/**
 * A CycloneDX SBOM, reduced to what the SBOM-freshness gate KPI (B3) needs: the format marker and
 * `metadata.timestamp`. SPHA has no general SBOM parser; this is deliberately minimal (the parser
 * ignores unknown keys, so a full `.cdx.json` deserializes fine).
 */
@Serializable
data class CycloneDxSbomDto(
    @SerialName("bomFormat") val bomFormat: String? = null,
    @SerialName("specVersion") val specVersion: String? = null,
    @SerialName("metadata") val metadata: CycloneDxMetadataDto? = null,
) : ToolResult, Origin

@Serializable
data class CycloneDxMetadataDto(@SerialName("timestamp") val timestamp: String? = null)
