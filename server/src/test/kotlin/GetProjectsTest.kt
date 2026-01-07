/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha

import de.fraunhofer.iem.spha.model.SphaToolResult
import de.fraunhofer.iem.spha.model.ToolInfoAndOrigin
import de.fraunhofer.iem.spha.model.adapter.ToolInfo
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode
import de.fraunhofer.iem.spha.model.project.Language
import de.fraunhofer.iem.spha.model.project.ProjectInfo
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class ProjectsEndpointTest {

    private fun createTestProjectInfo(
        name: String = "test-project",
        url: String = "https://github.com/test/test-project",
    ) =
        ProjectInfo(
            name = name,
            usedLanguages = listOf(Language("Kotlin", 1000), Language("Java", 500)),
            url = url,
            stars = 42,
            numberOfContributors = 5,
            numberOfCommits = 100,
            lastCommitDate = "2026-01-01T00:00:00Z",
        )

    private fun createTestKpiResultHierarchy(): KpiResultHierarchy {
        val leafNode =
            KpiResultNode(
                typeId = "test-leaf-kpi",
                result = KpiCalculationResult.Success(score = 85),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = emptyList(),
            )

        val rootNode =
            KpiResultNode(
                typeId = "test-root-kpi",
                result = KpiCalculationResult.Success(score = 85),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiResultEdge(target = leafNode, plannedWeight = 1.0, actualWeight = 1.0)
                    ),
            )

        return KpiResultHierarchy.create(rootNode)
    }

    private fun createTestSphaToolResult(
        projectName: String = "test-project",
        projectUrl: String = "https://github.com/test/test-project",
    ): SphaToolResult {
        val toolInfo =
            ToolInfo(
                name = "test-tool",
                description = "A test tool for unit testing",
                version = "1.0.0",
            )

        return SphaToolResult(
            resultHierarchy = createTestKpiResultHierarchy(),
            origins = listOf(ToolInfoAndOrigin(toolInfo = toolInfo, origins = emptyList())),
            projectInfo = createTestProjectInfo(name = projectName, url = projectUrl),
        )
    }

    @Test
    fun `GET to api-projects returns empty list when no projects exist`() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    }
                )
            }
        }

        val response = client.get("/api/projects")

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json { ignoreUnknownKeys = true }
        val responseMap = json.decodeFromString<Map<String, List<Int>>>(response.bodyAsText())
        assertTrue(responseMap.containsKey("projectIds"))
    }

    @Test
    fun `GET to api-projects returns project IDs after creating projects`() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    }
                )
            }
        }

        // Create two projects
        val timestamp = System.currentTimeMillis()
        val testResult1 =
            createTestSphaToolResult(
                projectName = "project-1-$timestamp",
                projectUrl = "https://github.com/test/project-1-$timestamp",
            )
        val testResult2 =
            createTestSphaToolResult(
                projectName = "project-2-$timestamp",
                projectUrl = "https://github.com/test/project-2-$timestamp",
            )

        client.post("/api/report") {
            contentType(ContentType.Application.Json)
            setBody(testResult1)
        }
        client.post("/api/report") {
            contentType(ContentType.Application.Json)
            setBody(testResult2)
        }

        // Get all project IDs
        val response = client.get("/api/projects")

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json { ignoreUnknownKeys = true }
        val responseMap = json.decodeFromString<Map<String, List<Int>>>(response.bodyAsText())
        assertTrue(responseMap.containsKey("projectIds"))
        assertTrue(responseMap["projectIds"]!!.size >= 2)
    }

    @Test
    fun `GET to api-projects-projectId-results returns empty list for non-existent project`() =
        testApplication {
            application { module() }

            val client = createClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        }
                    )
                }
            }

            val response = client.get("/api/projects/99999/results")

            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json { ignoreUnknownKeys = true }
            val responseMap =
                json.decodeFromString<Map<String, List<SphaToolResult>>>(response.bodyAsText())
            assertTrue(responseMap.containsKey("results"))
            assertEquals(0, responseMap["results"]!!.size)
        }

    @Test
    fun `GET to api-projects-projectId-results returns results for existing project`() =
        testApplication {
            application { module() }

            val client = createClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        }
                    )
                }
            }

            // Create a project with a result
            val timestamp = System.currentTimeMillis()
            val testResult =
                createTestSphaToolResult(
                    projectName = "test-project-$timestamp",
                    projectUrl = "https://github.com/test/project-$timestamp",
                )

            val createResponse =
                client.post("/api/report") {
                    contentType(ContentType.Application.Json)
                    setBody(testResult)
                }

            val json = Json { ignoreUnknownKeys = true }
            val createResponseMap =
                json.decodeFromString<Map<String, Int>>(createResponse.bodyAsText())
            val projectId = createResponseMap["projectId"]!!

            // Get results for the project
            val response = client.get("/api/projects/$projectId/results")

            assertEquals(HttpStatusCode.OK, response.status)
            val responseMap =
                json.decodeFromString<Map<String, List<SphaToolResult>>>(response.bodyAsText())
            assertTrue(responseMap.containsKey("results"))
            assertEquals(1, responseMap["results"]!!.size)

            val result = responseMap["results"]!![0]
            assertEquals(testResult.projectInfo.name, result.projectInfo.name)
            assertEquals(testResult.projectInfo.url, result.projectInfo.url)
            assertEquals(testResult.projectInfo.stars, result.projectInfo.stars)
        }

    @Test
    fun `GET to api-projects-projectId-results returns multiple results for same project`() =
        testApplication {
            application { module() }

            val client = createClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        }
                    )
                }
            }

            // Create a project with multiple results
            val timestamp = System.currentTimeMillis()
            val projectUrl = "https://github.com/test/multi-result-project-$timestamp"

            val testResult1 =
                createTestSphaToolResult(
                    projectName = "multi-result-project",
                    projectUrl = projectUrl,
                )
            val testResult2 =
                createTestSphaToolResult(
                    projectName = "multi-result-project",
                    projectUrl = projectUrl,
                )

            val createResponse1 =
                client.post("/api/report") {
                    contentType(ContentType.Application.Json)
                    setBody(testResult1)
                }
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(testResult2)
            }

            val json = Json { ignoreUnknownKeys = true }
            val createResponseMap =
                json.decodeFromString<Map<String, Int>>(createResponse1.bodyAsText())
            val projectId = createResponseMap["projectId"]!!

            // Get results for the project
            val response = client.get("/api/projects/$projectId/results")

            assertEquals(HttpStatusCode.OK, response.status)
            val responseMap =
                json.decodeFromString<Map<String, List<SphaToolResult>>>(response.bodyAsText())
            assertTrue(responseMap.containsKey("results"))
            assertEquals(2, responseMap["results"]!!.size)

            // Verify both results have the same project info
            responseMap["results"]!!.forEach { result ->
                assertEquals(testResult1.projectInfo.url, result.projectInfo.url)
            }
        }

    @Test
    fun `GET to api-projects-projectId-results with invalid project ID returns bad request`() =
        testApplication {
            application { module() }

            val response = client.get("/api/projects/invalid/results")

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `GET to api-projects-projectId-results preserves complete SphaToolResult structure`() =
        testApplication {
            application { module() }

            val client = createClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        }
                    )
                }
            }

            // Create a complex result with multiple origins
            val timestamp = System.currentTimeMillis()
            val projectUrl = "https://github.com/test/complex-structure-$timestamp"

            val complexResult =
                SphaToolResult(
                    resultHierarchy = createTestKpiResultHierarchy(),
                    origins =
                        listOf(
                            ToolInfoAndOrigin(
                                toolInfo =
                                    ToolInfo(
                                        name = "tool-1",
                                        description = "First tool",
                                        version = "1.0",
                                    ),
                                origins = emptyList(),
                            ),
                            ToolInfoAndOrigin(
                                toolInfo =
                                    ToolInfo(
                                        name = "tool-2",
                                        description = "Second tool",
                                        version = "2.0",
                                    ),
                                origins = emptyList(),
                            ),
                        ),
                    projectInfo =
                        createTestProjectInfo(name = "complex-structure", url = projectUrl),
                )

            val createResponse =
                client.post("/api/report") {
                    contentType(ContentType.Application.Json)
                    setBody(complexResult)
                }

            val json = Json { ignoreUnknownKeys = true }
            val createResponseMap =
                json.decodeFromString<Map<String, Int>>(createResponse.bodyAsText())
            val projectId = createResponseMap["projectId"]!!

            // Get results
            val response = client.get("/api/projects/$projectId/results")

            assertEquals(HttpStatusCode.OK, response.status)
            val responseMap =
                json.decodeFromString<Map<String, List<SphaToolResult>>>(response.bodyAsText())
            val result = responseMap["results"]!![0]

            // Verify structure is preserved
            assertEquals(2, result.origins.size)
            assertEquals("tool-1", result.origins[0].toolInfo.name)
            assertEquals("tool-2", result.origins[1].toolInfo.name)
            assertEquals(2, result.projectInfo.usedLanguages.size)
            assertEquals("Kotlin", result.projectInfo.usedLanguages[0].name)
        }
}
