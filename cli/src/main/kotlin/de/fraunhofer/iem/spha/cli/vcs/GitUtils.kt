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
    fun runGitCommand(workingDirectory: java.io.File? = null, vararg args: String): String? {
        return try {
            val processBuilder = ProcessBuilder("git", *args).redirectErrorStream(true)

            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory)
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
    fun detectGitRepositoryUrl(workingDirectory: java.io.File? = null): String? {
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
            logger.warn { "Unable to detect repository URL from git" }
        }
        return url
    }
}
