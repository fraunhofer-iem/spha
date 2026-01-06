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
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

@Serializable
private data class GitLabProject(
    val name: String,
    @SerialName("web_url") val webUrl: String,
    @SerialName("star_count") val starCount: Int,
    val languages: Map<String, Double>? = null,
)

@Serializable
private data class GitLabCommit(
    @SerialName("committed_date") val committedDate: String
)

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
     * @param repoUrl The GitLab repository URL
     * @param tokenOverride Optional token to override environment variable token
     * @return ProjectInfo containing repository details
     */
    override suspend fun getProjectInfo(repoUrl: String, tokenOverride: String?): NetworkResponse<ProjectInfo> {
        val token = tokenOverride ?: getToken()
        logger.info { "Fetching project information from GitLab for repository: $repoUrl" }

        val (host, projectPath) = parseGitLabUrl(repoUrl) 
            ?: return NetworkResponse.Failed("Invalid GitLab URL")
        logger.debug { "Parsed repository URL: host=$host, projectPath=$projectPath" }

        val encodedPath = withContext(Dispatchers.IO) {
            URLEncoder.encode(projectPath, "UTF-8")
        }
        val baseUrl = "https://$host/api/v4"

        try {
            // Fetch project details
            val projectResponse = gitlabApiClient.get("$baseUrl/projects/$encodedPath") {
                token?.let { header("PRIVATE-TOKEN", it) }
            }

            if (!projectResponse.status.isSuccess()) {
                return NetworkResponse.Failed(
                    "GitLab API returned status code ${projectResponse.status.value}"
                )
            }

            val project = projectResponse.body<GitLabProject>()

            // Fetch languages (separate endpoint)
            val languagesResponse = gitlabApiClient.get("$baseUrl/projects/$encodedPath/languages") {
                token?.let { header("PRIVATE-TOKEN", it) }
            }
            val languages = if (languagesResponse.status.isSuccess()) {
                languagesResponse.body<Map<String, Double>>()
            } else {
                emptyMap()
            }

            // Fetch contributors count
            val contributorsResponse = gitlabApiClient.get("$baseUrl/projects/$encodedPath/repository/contributors") {
                token?.let { header("PRIVATE-TOKEN", it) }
                parameter("per_page", "1")
            }
            val contributorsCount = contributorsResponse.headers["X-Total"]?.toIntOrNull() ?: -1

            // Fetch last commit
            val commitsResponse = gitlabApiClient.get("$baseUrl/projects/$encodedPath/repository/commits") {
                token?.let { header("PRIVATE-TOKEN", it) }
                parameter("per_page", "1")
            }
            
            var lastCommitDate: String? = null
            var totalCommits: Int? = null
            
            if (commitsResponse.status.isSuccess()) {
                val commits = commitsResponse.body<List<GitLabCommit>>()
                lastCommitDate = commits.firstOrNull()?.committedDate
                totalCommits = commitsResponse.headers["X-Total"]?.toIntOrNull()
            }

            logger.info { "Successfully fetched project information for ${project.name}" }

            return NetworkResponse.Success(
                ProjectInfo(
                    name = project.name,
                    usedLanguages = languages.map { (name, percentage) -> 
                        Language(name, percentage.toInt()) 
                    },
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

    /**
     * Parses a GitLab URL to extract host and project path.
     *
     * @param url The GitLab repository URL
     * @return A Pair containing the host and project path (e.g., "group/project")
     */
    private fun parseGitLabUrl(url: String): Pair<String, String>? {
        // Matches gitlab.com/group/project or custom-gitlab.com/group/subgroup/project
        val regex = Regex("""(?:https?://)?([^/]+)/(.+?)(?:\.git)?/?$""")
        val matchResult = regex.find(url) ?: return null

        val (host, projectPath) = matchResult.destructured
        
        // Only accept URLs that contain "gitlab" in the host
        if (!host.contains("gitlab", ignoreCase = true)) {
            return null
        }
        
        return Pair(host, projectPath)
    }

    /**
     * Gets the GitLab authentication token from environment variables.
     * Checks GITLAB_TOKEN first, then falls back to CI_JOB_TOKEN (GitLab CI).
     *
     * @return The GitLab token or null if not available
     */
    private fun getToken(): String? {
        return System.getenv("GITLAB_TOKEN") ?: System.getenv("CI_JOB_TOKEN")
    }
}
