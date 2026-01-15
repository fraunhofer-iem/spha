/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.project

import kotlinx.serialization.Serializable

@Serializable
data class ProjectInfo(
    val name: String,
    val usedLanguages: List<Language>,
    val url: String,
    val stars: Int,
    val numberOfContributors: Int,
    val numberOfCommits: Int?,
    val lastCommitDate: String?,
)

@Serializable data class Language(val name: String, val size: Int)
