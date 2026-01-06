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
     * Detects the repository URL from the current git repository.
     * Executes `git config --get remote.origin.url` to get the URL.
     *
     * @return The repository URL or null if unable to detect
     */
    fun detectGitRepositoryUrl(): String? {
        return try {
            val process = ProcessBuilder("git", "config", "--get", "remote.origin.url")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            
            if (process.exitValue() == 0 && output.isNotBlank()) {
                logger.info { "Detected repository URL from git: $output" }
                output
            } else {
                logger.warn { "Unable to detect repository URL from git" }
                null
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to detect repository URL from git: ${e.message}" }
            null
        }
    }
}
