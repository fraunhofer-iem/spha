/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import de.fraunhofer.iem.spha.model.project.ProjectInfo
import io.ktor.utils.io.core.Closeable

/**
 * Interface for fetching project information from various sources (GitHub, GitLab, local
 * repositories).
 */
interface ProjectInfoFetcher : Closeable {
    /**
     * Fetches project information from a repository.
     *
     * @param repoOrigin The repository URL or local path
     * @param tokenOverride Optional token to override environment variable token
     * @return NetworkResponse containing ProjectInfo or error
     */
    suspend fun getProjectInfo(
        repoOrigin: String,
        tokenOverride: String? = null,
    ): NetworkResponse<ProjectInfo>
}
