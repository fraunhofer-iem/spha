/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/** A class responsible for fetching project information from local git repositories. */
class LocalRepositoryFetcher(
    val logger: KLogger = KotlinLogging.logger {},
) : ProjectInfoFetcher {

    /**
     * Fetches project information from a local git repository.
     *
     * @param repoUrl The local repository path (can be relative or absolute)
     * @param tokenOverride Not used for local repositories
     * @return ProjectInfo containing repository details
     */
    override suspend fun getProjectInfo(repoUrl: String, tokenOverride: String?): NetworkResponse<ProjectInfo> {
        logger.info { "Fetching project information from local repository: $repoUrl" }

        val repoPath = Path.of(repoUrl).toAbsolutePath()
        
        if (!repoPath.exists() || !repoPath.isDirectory()) {
            return NetworkResponse.Failed("Repository path does not exist or is not a directory: $repoPath")
        }

        val gitDir = repoPath.resolve(".git")
        if (!gitDir.exists()) {
            return NetworkResponse.Failed("Not a git repository (no .git directory found): $repoPath")
        }

        try {
            val name = getRepositoryName(repoPath)
            val url = getRemoteUrl(repoPath) ?: repoPath.toString()
            val languages = detectLanguages(repoPath)
            val contributors = getContributorCount(repoPath)
            val commits = getCommitCount(repoPath)
            val lastCommitDate = getLastCommitDate(repoPath)

            logger.info { "Successfully fetched local repository information for $name" }

            return NetworkResponse.Success(
                ProjectInfo(
                    name = name,
                    usedLanguages = languages,
                    url = url,
                    numberOfContributors = contributors,
                    numberOfCommits = commits,
                    lastCommitDate = lastCommitDate,
                    stars = -1, // Not applicable for local repos
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch local repository info: ${e.message}" }
            return NetworkResponse.Failed("Failed to fetch local repository info: ${e.message}")
        }
    }

    private fun getRepositoryName(repoPath: Path): String {
        return repoPath.fileName?.toString() ?: "unknown"
    }

    private fun getRemoteUrl(repoPath: Path): String? {
        return GitUtils.runGitCommand(repoPath.toFile(), "config", "--get", "remote.origin.url")
    }

    private fun getContributorCount(repoPath: Path): Int {
        val output = GitUtils.runGitCommand(repoPath.toFile(), "shortlog", "-s", "-n", "--all") ?: return -1
        return output.lines().filter { it.isNotBlank() }.size
    }

    private fun getCommitCount(repoPath: Path): Int? {
        val output = GitUtils.runGitCommand(repoPath.toFile(), "rev-list", "--all", "--count") ?: return null
        return output.trim().toIntOrNull()
    }

    private fun getLastCommitDate(repoPath: Path): String? {
        return GitUtils.runGitCommand(repoPath.toFile(), "log", "-1", "--format=%cI")
    }

    private fun detectLanguages(repoPath: Path): List<Language> {
        val languageMap = mutableMapOf<String, Long>()
        
        // File extensions to language mapping (simplified)
        val extensionMap = mapOf(
            "kt" to "Kotlin",
            "java" to "Java",
            "py" to "Python",
            "js" to "JavaScript",
            "ts" to "TypeScript",
            "go" to "Go",
            "rs" to "Rust",
            "cpp" to "C++",
            "c" to "C",
            "cs" to "C#",
            "rb" to "Ruby",
            "php" to "PHP",
            "swift" to "Swift",
            "scala" to "Scala",
        )

        try {
            Files.walk(repoPath)
                .filter { Files.isRegularFile(it) }
                .filter { !it.toString().contains("/.git/") }
                .filter { !it.toString().contains("\\.git\\") }
                .forEach { file ->
                    val extension = file.fileName.toString().substringAfterLast('.', "")
                    val language = extensionMap[extension]
                    if (language != null) {
                        val size = Files.size(file)
                        languageMap[language] = languageMap.getOrDefault(language, 0L) + size
                    }
                }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to detect languages: ${e.message}" }
        }

        return languageMap.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { Language(it.key, it.value.toInt()) }
    }

    override fun close() {
        // Nothing to close for local repository
    }
}
