/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

/** Utility functions for Git operations. */
object GitUtils {
    private val logger = KotlinLogging.logger {}
    private const val gitExecutableProperty = "spha.git.executable"
    private const val windowsOsNamePrefix = "windows"
    private val unixGitCandidates =
        listOf(
            Path.of("/usr/bin/git"),
            Path.of("/usr/local/bin/git"),
            Path.of("/opt/homebrew/bin/git"),
            Path.of("/opt/local/bin/git"),
            Path.of("/bin/git"),
        )
    private val windowsGitCandidates =
        listOf(
            Path.of("C:\\Program Files\\Git\\cmd\\git.exe"),
            Path.of("C:\\Program Files\\Git\\bin\\git.exe"),
            Path.of("C:\\Program Files (x86)\\Git\\cmd\\git.exe"),
            Path.of("C:\\Program Files (x86)\\Git\\bin\\git.exe"),
        )

    @Volatile
    private var cachedGitExecutable: String? = null
    private var lastCheckedProperty: String? = null

    private fun resolveGitExecutable(): String? {
        val currentProperty = System.getProperty(gitExecutableProperty)?.trim()

        // Return cached value if system property hasn't changed
        if (currentProperty == lastCheckedProperty && cachedGitExecutable != null) {
            return cachedGitExecutable
        }

        lastCheckedProperty = currentProperty

        val resolved = if (!currentProperty.isNullOrEmpty()) {
            val configuredPath = Path.of(currentProperty)
            if (!configuredPath.isAbsolute) {
                logger.warn {
                    "Configured git executable '$currentProperty' is not absolute. " +
                        "Set -D$gitExecutableProperty to an absolute path."
                }
                null
            } else if (!Files.isRegularFile(configuredPath) || !Files.isExecutable(configuredPath)) {
                logger.warn {
                    "Configured git executable '$configuredPath' does not exist or is not executable."
                }
                null
            } else {
                configuredPath.toString()
            }
        } else {
            val candidates =
                if (System.getProperty("os.name").lowercase().startsWith(windowsOsNamePrefix)) {
                    windowsGitCandidates
                } else {
                    unixGitCandidates
                }
            candidates
                .firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }
                ?.toString()
        }

        cachedGitExecutable = resolved
        return resolved
    }

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
            val gitExecutable = resolveGitExecutable()
            if (gitExecutable == null) {
                logger.warn {
                    "No trusted absolute git executable found. Git command execution skipped."
                }
                return null
            }

            val processBuilder = ProcessBuilder(gitExecutable, *args).redirectErrorStream(true)

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
