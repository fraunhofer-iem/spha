/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
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
 * Represents an envelope format to link tool result files to the tool that produced the result.
 *
 * @property tool The identifier of the tool that produced the result (e.g., "osv-scanner",
 *   "trivy"). This is usually the name of the tool's binary
 * @property resultFile The path to the file containing the actual tool results.
 */
@Serializable
data class ToolResultEnvelope(
    @SerialName("tool") val tool: String,
    @SerialName("result_file") val resultFile: String,
)
