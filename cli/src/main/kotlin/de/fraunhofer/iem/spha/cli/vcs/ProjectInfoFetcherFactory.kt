/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/** Enum representing supported repository types. */
enum class RepositoryType {
    GITHUB,
    GITLAB,
    LOCAL;

    companion object {
        fun fromString(value: String): RepositoryType? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/** Factory for creating the appropriate ProjectInfoFetcher based on repository URL/path. */
object ProjectInfoFetcherFactory {
    private val logger = KotlinLogging.logger {}


    /**
     * Creates a repository info provider based on an explicit repository type.
     *
     * @param repositoryType The repository type: "github", "gitlab", or "local"
     * @return ProjectInfoFetcher instance for the specified type
     * @throws IllegalArgumentException if repositoryType is invalid
     */
    fun createFetcher(repositoryType: RepositoryType): ProjectInfoFetcher {
        logger.info { "Creating repository info provider for type: $repositoryType" }
        return when (repositoryType) {
            RepositoryType.GITHUB -> GitHubProjectFetcher()
            RepositoryType.GITLAB -> GitLabProjectFetcher()
            RepositoryType.LOCAL -> LocalRepositoryFetcher()
        }
    }

    /**
     * Creates a repository info provider by parsing the repository type from a string.
     *
     * @param repositoryType The repository type name: "github", "gitlab", or "local"
     * @return ProjectInfoFetcher instance for the specified type
     * @throws IllegalArgumentException if repositoryType is invalid
     */
    fun createFetcher(repositoryType: String): ProjectInfoFetcher {
        val type = RepositoryType.fromString(repositoryType)
            ?: throw IllegalArgumentException("Invalid repository type: $repositoryType. Valid values: github, gitlab, local")
        return createFetcher(type)
    }

    /**
     * Detects the repository type based on the repository URL or path.
     *
     * @param repoUrlOrPath The repository URL or local path
     * @return RepositoryType
     * @throws IllegalArgumentException if unable to detect repository type
     */
    fun detectRepositoryType(repoUrlOrPath: String): RepositoryType {
        logger.debug { "Determining repository type for: $repoUrlOrPath" }

        return when {
            // Check if it's a local path first
            isLocalRepository(repoUrlOrPath) -> {
                logger.info { "Detected local repository" }
                RepositoryType.LOCAL
            }
            // Parse as remote URL
            else -> {
                val host = parseHostFromUrl(repoUrlOrPath)
                when {
                    host != null && host.equals("github.com", ignoreCase = true) -> {
                        logger.info { "Detected GitHub repository from host: $host" }
                        RepositoryType.GITHUB
                    }
                    host != null && host.contains("gitlab", ignoreCase = true) -> {
                        logger.info { "Detected GitLab repository from host: $host" }
                        RepositoryType.GITLAB
                    }
                    host != null -> {
                        logger.error { "Unknown remote repository host '$host'. Use --repositoryType to specify the repository type." }
                        throw IllegalArgumentException(
                            "Unable to determine repository type for '$repoUrlOrPath'. " +
                            "Unknown host: '$host'. " +
                            "Please specify the repository type using --repositoryType option (github, gitlab, or local)."
                        )
                    }
                    else -> {
                        logger.info { "Unable to parse as URL, assuming local repository path" }
                        RepositoryType.LOCAL
                    }
                }
            }
        }
    }

    /**
     * Creates the appropriate repository info provider based on the repository URL or path (auto-detection).
     *
     * @param repoUrlOrPath The repository URL or local path
     * @return ProjectInfoFetcher instance for the detected repository type
     * @throws IllegalArgumentException if repository type cannot be determined
     */
    fun createFetcherFromUrl(repoUrlOrPath: String): ProjectInfoFetcher {
        val repositoryType = detectRepositoryType(repoUrlOrPath)
        return createFetcher(repositoryType)
    }

    /**
     * Parses the host from a repository URL (supports HTTP(S) and SSH git URLs).
     *
     * @param url The repository URL
     * @return The host name or null if unable to parse
     */
    private fun parseHostFromUrl(url: String): String? {
        return try {
            when {
                // Handle git@host:path SSH URLs
                url.startsWith("git@") -> {
                    url.substringAfter("git@").substringBefore(":")
                }
                // Handle HTTP(S) URLs
                url.startsWith("http://") || url.startsWith("https://") -> {
                    URI(url).host
                }
                // Try as URI anyway
                else -> {
                    URI(url).host
                }
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse URL: $url" }
            null
        }
    }

    /**
     * Checks if the given path is a local repository.
     *
     * @param path The path to check
     * @return true if it's a local directory with a .git folder
     */
    private fun isLocalRepository(path: String): Boolean {
        // URLs are not local paths
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("git@")) {
            return false
        }

        return try {
            val repoPath = Path.of(path).toAbsolutePath()
            repoPath.exists() && repoPath.isDirectory() && repoPath.resolve(".git").exists()
        } catch (e: Exception) {
            false
        }
    }
}
