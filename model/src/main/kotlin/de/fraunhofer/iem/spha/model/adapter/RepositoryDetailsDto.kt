/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

data class RepositoryDetailsDto(
    val projectId: Long,
    val numberOfCommits: Int,
    val numberOfSignedCommits: Int,
    val isDefaultBranchProtected: Boolean,
) : Origin, ToolResult
