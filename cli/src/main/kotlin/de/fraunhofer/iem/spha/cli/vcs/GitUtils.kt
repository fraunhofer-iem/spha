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
import java.nio.file.Path

/** Utility functions for Git operations. */
object GitUtils {
    private val logger = KotlinLogging.logger {}

    /**
     * Executes a git command in a specific directory.
     *
     * @param workingDirectory The directory to execute the git command in, or null for current
     *   directory
     * @param args The git command arguments (without "git" prefix)
     * @return The command output or null if command failed
     */
    fun runGitCommand(workingDirectory: Path? = null, vararg args: String): String? {
        return try {
            val processBuilder = ProcessBuilder("git", *args).redirectErrorStream(true)

            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory.toFile())
            }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            if (process.exitValue() == 0 && output.isNotBlank()) {
                output
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug(e) { "Git command failed: ${e.message}" }
            null
        }
    }

    /**
     * Detects the repository URL from the current git repository. Executes `git config --get
     * remote.origin.url` to get the URL.
     *
     * @param workingDirectory The directory to detect the repository URL in, or null for current
     *   directory
     * @return The repository URL or null if unable to detect
     */
    fun detectGitRepositoryUrl(workingDirectory: Path? = null): String? {
        val url =
            runGitCommand(
                workingDirectory = workingDirectory,
                "config",
                "--get",
                "remote.origin.url",
            )
        if (url != null) {
            logger.info { "Detected repository URL from git: $url" }
        } else {
            logger.trace { "Unable to detect repository URL from git" }
        }
        return url
    }

    /**
     * Gets the current commit SHA from a git repository.
     *
     * @param workingDirectory The repository directory to read from, or null for current directory
     * @return The current commit SHA or null if unavailable
     */
    fun getCurrentCommitSha(workingDirectory: Path? = null): String? {
        val sha = runGitCommand(workingDirectory = workingDirectory, "rev-parse", "HEAD")
        if (sha != null) {
            logger.info { "Detected commit SHA from git: $sha" }
        } else {
            logger.trace { "Unable to detect commit SHA from git" }
        }
        return sha
    }

    /**
     * Parses a VCS URL into host and path parts. Supports HTTPS, HTTP, and SSH (git@host:path)
     * formats. Host is normalized (lowercase, 'www.' prefix removed).
     *
     * @param url The VCS URL to parse
     * @return Pair of host and path (without .git suffix), or null if parsing fails
     */
    fun parseVCSUrl(url: String): Pair<String, String>? {
        return try {
            when {
                // git@host:path
                url.startsWith("git@") -> {
                    val host =
                        url.substringAfter("git@")
                            .substringBefore(":")
                            .lowercase()
                            .removePrefix("www.")
                    val path = url.substringAfter(":").removeSuffix(".git").trim('/')
                    host to path
                }
                // http(s)://host/path
                url.startsWith("http://") || url.startsWith("https://") -> {
                    val uri = java.net.URI(url)
                    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
                    val path = uri.path.trim('/').removeSuffix(".git")
                    host to path
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
