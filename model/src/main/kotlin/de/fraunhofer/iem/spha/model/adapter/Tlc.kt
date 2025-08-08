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
data class TlcDto(
    val optional: Tlc,
    val production: Tlc,
    val directOptional: Tlc,
    val directProduction: Tlc,
) : ToolResult

@Serializable
data class Tlc(
    val libdays: Double,
    val missedReleases: Int,
    val numComponents: Int,
    val highestLibdays: Double,
    val highestMissedReleases: Int,
    val componentHighestMissedReleases: Component,
    val componentHighestLibdays: Component,
) : Origin

@Serializable
data class Component(
    @SerialName("bom-ref") val bomRef: String,
    val type: String = "",
    val name: String = "",
    val version: String = "",
    val description: String = "",
    val scope: String = "",
    val purl: String = "",
    val licenses: List<LicenseWrapper> = emptyList(),
)

@Serializable data class LicenseWrapper(val license: License)

@Serializable data class License(val id: String = "")
