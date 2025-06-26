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
