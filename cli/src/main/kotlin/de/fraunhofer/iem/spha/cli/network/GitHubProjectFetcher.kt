/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.network

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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable private data class GraphQLRequest(@SerialName("query") val query: String)

@Serializable
private data class GraphQLResponse<T>(val data: T?, val errors: List<GraphQLError>? = null)

@Serializable private data class GraphQLError(val message: String)

@Serializable private data class RepositoryData(val repository: Repository?)

@Serializable
private data class Repository(
    val name: String,
    val url: String,
    val stargazerCount: Int,
    val languages: LanguageConnection,
    val defaultBranchRef: BranchRef?,
    val collaborators: UserConnection,
)

@Serializable private data class LanguageConnection(val edges: List<LanguageEdge>)

@Serializable private data class LanguageEdge(val size: Int, val node: LanguageNode)

@Serializable private data class LanguageNode(val name: String)

@Serializable private data class BranchRef(val target: Commit?)

@Serializable private data class Commit(val history: CommitHistory, val committedDate: String)

@Serializable private data class CommitHistory(val totalCount: Int)

@Serializable private data class UserConnection(val totalCount: Int)

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

/** A class responsible for fetching project information from GitHub repositories. */
class GitHubProjectFetcher(
    val logger: KLogger = KotlinLogging.logger {},
    private val githubApiClient: HttpClient = createDefaultHttpClient(),
) : Closeable by githubApiClient {

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
     * Fetches project information from a GitHub repository URL.
     *
     * @param repoUrl The GitHub repository URL
     * @return ProjectInfo containing repository details
     */
    suspend fun getProjectInfo(repoUrl: String, token: String): NetworkResponse<ProjectInfo> {
        logger.info { "Fetching project information from GitHub for repository: $repoUrl" }

        val (owner, repo) =
            parseGitHubUrl(repoUrl) ?: return NetworkResponse.Failed("Invalid repoUrl")
        logger.debug { "Parsed repository URL: owner=$owner, repo=$repo" }

        // Create the GraphQL query
        val query =
            """
            query {
              repository(owner: "$owner", name: "$repo") {
                name
                url
                stargazerCount
                languages(first: 10, orderBy: {field: SIZE, direction: DESC}) {
                  edges {
                    size
                    node {
                      name
                    }
                  }
                }
                defaultBranchRef {
                  target {
                    ... on Commit {
                      history {
                        totalCount
                      }
                      committedDate
                    }
                  }
                }
                collaborators {
                  totalCount
                }
              }
            }
        """
                .trimIndent()

        val response = executeGitHubGraphQLQuery(query, token)

        return when (response) {
            is NetworkResponse.Failed -> response
            is NetworkResponse.Success -> processGitHubResponse(response.data)
        }
    }

    /**
     * Parses a GitHub URL to extract owner and repository name.
     *
     * @param url The GitHub repository URL
     * @return A Pair containing the owner and repository name
     */
    private fun parseGitHubUrl(url: String): Pair<String, String>? {
        val regex = Regex("""github\.com[/:]([^/]+)/([^/.]+)(?:\.git)?""")
        val matchResult = regex.find(url) ?: return null

        // Using destructuring for clarity
        val (owner, repo) = matchResult.destructured
        return Pair(owner, repo)
    }

    /**
     * Executes a GraphQL query against the GitHub API.
     *
     * @param query The GraphQL query to execute
     * @param token GitHub API token for authentication
     * @return The response from the GitHub API
     */
    private suspend fun executeGitHubGraphQLQuery(
        query: String,
        token: String,
    ): NetworkResponse<GraphQLResponse<RepositoryData>> {

        logger.debug { "Sending GraphQL query to GitHub API" }

        val response =
            githubApiClient.post("https://api.github.com/graphql") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(GraphQLRequest(query))
            }

        if (!response.status.isSuccess()) {
            val responseBody = response.body<String>()
            return NetworkResponse.Failed(
                "GitHub API returned status code ${response.status.value}: $responseBody"
            )
        }

        // Ktor deserializes the entire JSON response into your data classes
        return NetworkResponse.Success(response.body<GraphQLResponse<RepositoryData>>())
    }

    /**
     * Processes the GitHub API response and converts it to ProjectInfo.
     *
     * @param response The response from the GitHub API
     * @return ProjectInfo containing repository details
     */
    private fun processGitHubResponse(
        response: GraphQLResponse<RepositoryData>
    ): NetworkResponse<ProjectInfo> {
        // Check for GraphQL-level errors
        response.errors?.let { errors ->
            val errorMessage = errors.joinToString(", ") { it.message }
            return NetworkResponse.Failed("GitHub API error: $errorMessage")
        }

        val repository =
            response.data?.repository
                ?: return NetworkResponse.Failed("Repository data not found in GitHub API response")

        logger.info { "Successfully fetched project information for ${repository.name}" }

        return NetworkResponse.Success(
            ProjectInfo(
                name = repository.name,
                usedLanguages = repository.languages.edges.map { Language(it.node.name, it.size) },
                url = repository.url,
                numberOfContributors = repository.collaborators.totalCount,
                numberOfCommits = repository.defaultBranchRef?.target?.history?.totalCount,
                lastCommitDate = repository.defaultBranchRef?.target?.committedDate,
                stars = repository.stargazerCount,
            )
        )
    }
}
