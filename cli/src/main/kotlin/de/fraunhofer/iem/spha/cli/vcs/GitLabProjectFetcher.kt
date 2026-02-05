/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import de.fraunhofer.iem.spha.model.project.Language
import de.fraunhofer.iem.spha.model.project.ProjectInfo
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class GitLabProject(
    val name: String,
    @SerialName("web_url") val webUrl: String,
    @SerialName("star_count") val starCount: Int,
    val languages: Map<String, Double>? = null,
)

@Serializable
private data class GitLabCommit(@SerialName("committed_date") val committedDate: String)

/** A class responsible for fetching project information from GitLab repositories. */
class GitLabProjectFetcher(
    val logger: KLogger = KotlinLogging.logger {},
    private val gitlabApiClient: HttpClient = createDefaultHttpClient(),
) : ProjectInfoFetcher, Closeable by gitlabApiClient {

    companion object {
        private fun createDefaultHttpClient() =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        }
                    )
                }
            }
    }

    /**
     * Fetches project information from a GitLab repository URL.
     *
     * @param repoOrigin The GitLab repository URL
     * @param tokenOverride Optional token to override environment variable token
     * @return ProjectInfo containing repository details
     */
    override suspend fun getProjectInfo(
        repoOrigin: String,
        tokenOverride: String?,
        commitSha: String?,
    ): NetworkResponse<ProjectInfo> {
        val token = tokenOverride ?: getToken()
        logger.info { "Fetching project information from GitLab for repository: $repoOrigin" }

        val (host, projectPath) =
            parseGitLabUrl(repoOrigin) ?: return NetworkResponse.Failed("Invalid GitLab URL")
        logger.debug { "Parsed repository URL: host=$host, projectPath=$projectPath" }

        val protocol = if (repoOrigin.startsWith("http://")) "http" else "https"
        val encodedPath = withContext(Dispatchers.IO) { URLEncoder.encode(projectPath, "UTF-8") }
        val baseUrl = "$protocol://$host/api/v4"
        val commitRef = commitSha?.takeIf { it.isNotBlank() }

        try {
            // Fetch project details
            val projectResponse =
                gitlabApiClient.get("$baseUrl/projects/$encodedPath") {
                    token?.let { header("PRIVATE-TOKEN", it) }
                }

            if (!projectResponse.status.isSuccess()) {
                return NetworkResponse.Failed(
                    "GitLab API returned status code ${projectResponse.status.value}"
                )
            }

            val project = projectResponse.body<GitLabProject>()

            // Fetch languages (separate endpoint)
            val languagesResponse =
                gitlabApiClient.get("$baseUrl/projects/$encodedPath/languages") {
                    token?.let { header("PRIVATE-TOKEN", it) }
                }
            val languages =
                if (languagesResponse.status.isSuccess()) {
                    languagesResponse.body<Map<String, Double>>()
                } else {
                    emptyMap()
                }

            // Fetch contributors count
            val contributorsResponse =
                gitlabApiClient.get("$baseUrl/projects/$encodedPath/repository/contributors") {
                    token?.let { header("PRIVATE-TOKEN", it) }
                    parameter("per_page", "1")
                    commitRef?.let { parameter("ref_name", it) }
                }
            val contributorsCount = contributorsResponse.headers["X-Total"]?.toIntOrNull() ?: -1

            // Fetch last commit
            val commitsResponse =
                gitlabApiClient.get("$baseUrl/projects/$encodedPath/repository/commits") {
                    token?.let { header("PRIVATE-TOKEN", it) }
                    parameter("per_page", "1")
                    commitRef?.let { parameter("ref_name", it) }
                }

            var lastCommitDate: String? = null
            var totalCommits: Int? = null

            if (commitsResponse.status.isSuccess()) {
                val commits = commitsResponse.body<List<GitLabCommit>>()
                lastCommitDate = commits.firstOrNull()?.committedDate
                totalCommits = commitsResponse.headers["X-Total"]?.toIntOrNull()
            }

            val commitDateAtSha =
                commitRef?.let { fetchCommitDate(baseUrl, encodedPath, token, it) }
            if (commitDateAtSha != null) {
                lastCommitDate = commitDateAtSha
            }

            logger.info { "Successfully fetched project information for ${project.name}" }

            return NetworkResponse.Success(
                ProjectInfo(
                    name = project.name,
                    usedLanguages =
                        languages.map { (name, percentage) -> Language(name, percentage.toInt()) },
                    url = project.webUrl,
                    numberOfContributors = contributorsCount,
                    numberOfCommits = totalCommits,
                    lastCommitDate = lastCommitDate,
                    stars = project.starCount,
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch GitLab project info: ${e.message}" }
            return NetworkResponse.Failed("Failed to fetch GitLab project info: ${e.message}")
        }
    }

    private suspend fun fetchCommitDate(
        baseUrl: String,
        encodedPath: String,
        token: String?,
        commitRef: String,
    ): String? {
        val response =
            gitlabApiClient.get("$baseUrl/projects/$encodedPath/repository/commits/$commitRef") {
                token?.let { header("PRIVATE-TOKEN", it) }
            }

        if (!response.status.isSuccess()) {
            return null
        }

        return response.body<GitLabCommit>().committedDate
    }

    /**
     * Parses a GitLab URL to extract host and project path.
     *
     * @param url The GitLab repository URL
     * @return A Pair containing the host and project path (e.g., "group/project")
     */
    private fun parseGitLabUrl(url: String): Pair<String, String>? {
        val (host, path) = GitUtils.parseVCSUrl(url) ?: return null

        // Only accept URLs that contain "gitlab" in the host
        if (!host.contains("gitlab", ignoreCase = true)) {
            return null
        }

        return Pair(host, path)
    }

    /**
     * Gets the GitLab authentication token from environment variables. Checks GITLAB_TOKEN first,
     * then falls back to CI_JOB_TOKEN (GitLab CI).
     *
     * @return The GitLab token or null if not available
     */
    private fun getToken(): String? {
        return System.getenv("GITLAB_TOKEN") ?: System.getenv("CI_JOB_TOKEN")
    }
}
