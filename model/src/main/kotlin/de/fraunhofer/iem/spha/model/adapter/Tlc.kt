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
    val transitiveOptional: Tlc,
    val transitiveProduction: Tlc,
    val directOptional: Tlc,
    val directProduction: Tlc,
) : ToolResult

@Serializable
data class Tlc(
    val totalNumComponents: Int = 0,
    val highestLibdays: Double,
    val componentHighestLibdays: Component? = null,
    val components: List<ComponentLag> = emptyList(),
) : TlcOrigin

sealed interface TlcOrigin : Origin

@Serializable
data class ComponentLag(val component: Component, val technicalLag: TechnicalLag) : TlcOrigin

@Serializable data class TechnicalLag(val libdays: Double)

@Serializable
data class Component(
    @SerialName("bom-ref") val bomRef: String? = "",
    @SerialName("type") val componentType: String? = "",
    val name: String? = "",
    val version: String? = "",
    val description: String? = "",
    val scope: String? = "",
    val purl: String? = "",
    val licenses: List<LicenseWrapper>? = emptyList(),
) : TlcOrigin

@Serializable data class LicenseWrapper(val license: License? = null)

@Serializable data class License(val id: String = "")
