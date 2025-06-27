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
data class OsvScannerDto(@SerialName("results") val results: List<OsvScannerResultDto>) :
    ToolResult

@Serializable
data class OsvScannerResultDto(
    @SerialName("source") val packageSource: PackageSource,
    @SerialName("packages") val packages: List<OsvPackageWrapperDto>,
)

@Serializable
data class PackageSource(
    @SerialName("path") val path: String,
    @SerialName("type") val type: String,
)

@Serializable
data class OsvPackageWrapperDto(
    @SerialName("package") val osvPackage: OsvPackageDto,
    @SerialName("vulnerabilities") val vulnerabilities: List<OsvVulnerabilityDto>,
    @SerialName("groups") val groups: List<GroupDto>,
)

@Serializable
data class GroupDto(
    @SerialName("ids") val ids: List<String>,
    @SerialName("aliases") val aliases: List<String>,
    @SerialName("max_severity") val maxSeverity: String,
)

@Serializable
data class OsvPackageDto(
    @SerialName("name") val name: String,
    @SerialName("version") val version: String,
    @SerialName("ecosystem") val ecosystem: String,
)

@Serializable
data class OsvVulnerabilityDto(
    @SerialName("affected") val affected: List<Affected>,
    @SerialName("severity") val severity: List<Severity>? = null,
    @SerialName("details") val details: String,
    @SerialName("id") val id: String,
    @SerialName("modified") val modified: String,
    @SerialName("published") val published: String,
    @SerialName("references") val references: List<Reference>,
    @SerialName("schema_version") val schemaVersion: String,
    @SerialName("summary") val summary: String,
) : Origin

@Serializable
data class Event(
    @SerialName("fixed") val fixed: String? = null,
    @SerialName("introduced") val introduced: String? = null,
)

@Serializable
data class Affected(
    @SerialName("package") val packageX: Package,
    @SerialName("ranges") val osvVulnerabilityRanges: List<OsvVulnerabilityRange>? = null,
)

@Serializable
data class Severity(
    //    Severity Type	Score Description
    //    CVSS_V2	A CVSS vector string representing the unique characteristics and severity of the
    // vulnerability using a version of the Common Vulnerability Scoring System notation that is ==
    // 2.0 (e.g."AV:L/AC:M/Au:N/C:N/I:P/A:C").
    //
    // CVSS_V3	A CVSS vector string representing the unique characteristics and severity of the
    // vulnerability using a version of the Common Vulnerability Scoring System notation that is >=
    // 3.0 and < 4.0 (e.g."CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:C/C:H/I:N/A:N").
    //
    // CVSS_V4	A CVSS vector string representing the unique characteristics and severity of the
    // vulnerability using a version on the Common Vulnerability Scoring System notation that is >=
    // 4.0 and < 5.0 (e.g. "CVSS:4.0/AV:N/AC:L/AT:N/PR:H/UI:N/VC:L/VI:L/VA:N/SC:N/SI:N/SA:N").
    @SerialName("type") val type: String,
    @SerialName("score") val score: String,
)

@Serializable
data class Package(
    @SerialName("ecosystem") val ecosystem: String,
    @SerialName("name") val name: String,
)

@Serializable
@SerialName("range")
data class OsvVulnerabilityRange(
    @SerialName("events") val events: List<Event>,
    @SerialName("repo") val repo: String? = null,
    @SerialName("type") val type: String,
)

@Serializable
data class Reference(@SerialName("type") val type: String, @SerialName("url") val url: String)
