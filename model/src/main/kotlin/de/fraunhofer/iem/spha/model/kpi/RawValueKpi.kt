/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class RawValueKpi(
    // Used to identify the KPI type of the given instance
    val typeId: String,
    // The calculated score based on the results
    val score: Int,
    // Used to uniquely identify the given instance
    val id: String = UUID.randomUUID().toString(),
    // [optional] Connects this instance to the result on which it's based
    val originId: String? = null,
)
