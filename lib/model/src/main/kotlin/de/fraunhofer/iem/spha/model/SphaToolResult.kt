/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model

import de.fraunhofer.iem.spha.model.adapter.Origin
import de.fraunhofer.iem.spha.model.adapter.ToolInfo
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.project.ProjectInfo
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SphaToolResult(
    val resultHierarchy: KpiResultHierarchy,
    val origins: List<ToolInfoAndOrigin>,
    val projectInfo: ProjectInfo,
    val commitSha: String,
    val createdAt: Instant,
)

@Serializable data class ToolInfoAndOrigin(val toolInfo: ToolInfo, val origins: List<Origin>)
