/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter.trivy

import de.fraunhofer.iem.spha.model.adapter.vulnerability.VulnerabilityDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

data class TrivyDto(val vulnerabilities: Collection<VulnerabilityDto>)

@Serializable
data class TrivyDtoV1(
    @SerialName("Vulnerabilities") val vulnerabilities: List<TrivyVulnerabilityDto> = listOf()
)

@Serializable
data class TrivyDtoV2(
    @SerialName("Results") val results: List<Result> = listOf(),
    @SerialName("SchemaVersion") val schemaVersion: Int,
)

@Serializable
data class Result(
    @SerialName("Vulnerabilities") val vulnerabilities: List<TrivyVulnerabilityDto> = listOf(),
    @SerialName("Licenses") val licenses: List<TrivyLicenseDto> = listOf(),
    @SerialName("Misconfigurations") val misconfigurations: List<TrivyMisconfigDto> = listOf(),
    @SerialName("Secrets") val secrets: List<TrivySecretDto> = listOf(),
)

@Serializable
data class TrivyVulnerabilityDto(
    // NB: Because the names of its inner elements are not fixed, this needs to be a JsonObject.
    // This way we can iterate over those when required. Their type is always CVSSData.
    @SerialName("CVSS") val cvss: JsonObject?,
    @SerialName("VulnerabilityID") val vulnerabilityID: String,
    @SerialName("InstalledVersion") val installedVersion: String,
    @SerialName("PkgName") val pkgName: String,
    /**
     * | Base Score Range | Severity |
     * |------------------|----------|
     * | 0.1-3.9          | Low      |
     * | 4.0-6.9          | Medium   |
     * | 7.0-8.9          | High     |
     * | 9.0-10.0         | Critical |
     */
    @SerialName("Severity") val severity: String,
)

@Serializable
data class CVSSData(
    @SerialName("V2Score") val v2Score: Double?,
    @SerialName("V3Score") val v3Score: Double?,
)

@Serializable
data class TrivyLicenseDto(
    // License are classified using the Google License Classification:
    /**
     * | Classification | Severity |
     * |----------------|----------|
     * | Forbidden      | CRITICAL |
     * | Restricted     | HIGH     |
     * | Reciprocal     | MEDIUM   |
     * | Notice         | LOW      |
     * | Permissive     | LOW      |
     * | Unencumbered   | LOW      |
     * | Unknown        | UNKNOWN  |
     */
    @SerialName("Severity") val severity: String,
    @SerialName("Category") val category: String,
    @SerialName("PkgName") val pkgName: String,
    @SerialName("Name") val name: String,
)

@Serializable
data class TrivyMisconfigDto(
    @SerialName("Severity") val severity: String,
    @SerialName("ID") val id: String,
    @SerialName("Title") val title: String,
)

@Serializable
data class TrivySecretDto(
    @SerialName("Severity") val severity: String,
    @SerialName("Category") val category: String,
    @SerialName("Title") val title: String,
)
