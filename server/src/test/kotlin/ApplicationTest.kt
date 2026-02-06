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

class ReportEndpointTest {

    init {
        TestDatabaseSetup.setupDatabase()
    }

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
        commitSha: String = "test-sha",
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
            commitSha = commitSha,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()),
        )
    }

    @Test
    fun `POST to api-report with valid SphaToolResult returns Created status`() = testApplication {
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

        val testResult = createTestSphaToolResult()

        val response =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(testResult)
            }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"))
        assertTrue(responseBody.contains("projectId"))
    }

    @Test
    fun `POST to api-report creates project and returns valid IDs`() = testApplication {
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

        val testResult =
            createTestSphaToolResult(
                projectName = "unique-project-${System.currentTimeMillis()}",
                projectUrl = "https://github.com/test/unique-project-${System.currentTimeMillis()}",
            )

        val response =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(testResult)
            }

        assertEquals(HttpStatusCode.Created, response.status)

        val json = Json { ignoreUnknownKeys = true }
        val responseMap = json.decodeFromString<Map<String, Int>>(response.bodyAsText())

        assertTrue(responseMap.containsKey("id"))
        assertTrue(responseMap.containsKey("projectId"))
        assertTrue(responseMap["id"]!! > 0)
        assertTrue(responseMap["projectId"]!! > 0)
    }

    @Test
    fun `POST to api-report with same project reuses existing project ID`() = testApplication {
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

        val projectName = "reused-project-${System.currentTimeMillis()}"
        val projectUrl = "https://github.com/test/reused-project"

        val testResult1 =
            createTestSphaToolResult(projectName = projectName, projectUrl = projectUrl)
        val testResult2 =
            createTestSphaToolResult(projectName = projectName, projectUrl = projectUrl)

        val response1 =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(testResult1)
            }

        val response2 =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(testResult2)
            }

        assertEquals(HttpStatusCode.Created, response1.status)
        assertEquals(HttpStatusCode.Created, response2.status)

        val json = Json { ignoreUnknownKeys = true }
        val responseMap1 = json.decodeFromString<Map<String, Int>>(response1.bodyAsText())
        val responseMap2 = json.decodeFromString<Map<String, Int>>(response2.bodyAsText())

        assertEquals(
            responseMap1["projectId"],
            responseMap2["projectId"],
            "Same project should have same projectId",
        )
        assertTrue(
            responseMap1["id"] != responseMap2["id"],
            "Different results should have different IDs",
        )
    }

    @Test
    fun `POST to api-report with minimal SphaToolResult succeeds`() = testApplication {
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

        val minimalProjectInfo =
            ProjectInfo(
                name = "minimal-project",
                usedLanguages = emptyList(),
                url = "https://github.com/test/minimal",
                stars = 0,
                numberOfContributors = 1,
                numberOfCommits = null,
                lastCommitDate = null,
            )

        val minimalNode =
            KpiResultNode(
                typeId = "minimal-kpi",
                result = KpiCalculationResult.Empty(),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = emptyList(),
            )

        val minimalResult =
            SphaToolResult(
                resultHierarchy = KpiResultHierarchy.create(minimalNode),
                origins = emptyList(),
                projectInfo = minimalProjectInfo,
                commitSha = "minimal-sha",
                createdAt = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            )

        val response =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(minimalResult)
            }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST to api-report with complex hierarchy succeeds`() = testApplication {
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

        val leaf1 =
            KpiResultNode(
                typeId = "security-kpi",
                result = KpiCalculationResult.Success(score = 90),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = emptyList(),
            )

        val leaf2 =
            KpiResultNode(
                typeId = "quality-kpi",
                result = KpiCalculationResult.Success(score = 75),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = emptyList(),
            )

        val leaf3 =
            KpiResultNode(
                typeId = "incomplete-kpi",
                result = KpiCalculationResult.Incomplete(score = 50, reason = "Missing data"),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = emptyList(),
            )

        val middleNode =
            KpiResultNode(
                typeId = "aggregated-kpi",
                result = KpiCalculationResult.Success(score = 82),
                strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
                edges =
                    listOf(
                        KpiResultEdge(target = leaf1, plannedWeight = 0.6, actualWeight = 0.6),
                        KpiResultEdge(target = leaf2, plannedWeight = 0.4, actualWeight = 0.4),
                    ),
            )

        val rootNode =
            KpiResultNode(
                typeId = "root-kpi",
                result = KpiCalculationResult.Success(score = 70),
                strategy = KpiStrategyId.MINIMUM_STRATEGY,
                edges =
                    listOf(
                        KpiResultEdge(target = middleNode, plannedWeight = 0.7, actualWeight = 0.7),
                        KpiResultEdge(target = leaf3, plannedWeight = 0.3, actualWeight = 0.3),
                    ),
            )

        val complexResult =
            SphaToolResult(
                resultHierarchy = KpiResultHierarchy.create(rootNode),
                origins =
                    listOf(
                        ToolInfoAndOrigin(
                            toolInfo =
                                ToolInfo(
                                    name = "security-scanner",
                                    description = "Scans for vulnerabilities",
                                ),
                            origins = emptyList(),
                        ),
                        ToolInfoAndOrigin(
                            toolInfo =
                                ToolInfo(
                                    name = "code-analyzer",
                                    description = "Analyzes code quality",
                                    version = "2.0",
                                ),
                            origins = emptyList(),
                        ),
                    ),
                projectInfo = createTestProjectInfo(name = "complex-project"),
                commitSha = "complex-sha",
                createdAt = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            )

        val response =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(complexResult)
            }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST to api-report with error result type succeeds`() = testApplication {
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

        val errorNode =
            KpiResultNode(
                typeId = "error-kpi",
                result = KpiCalculationResult.Error(reason = "Failed to calculate KPI"),
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = emptyList(),
            )

        val errorResult =
            SphaToolResult(
                resultHierarchy = KpiResultHierarchy.create(errorNode),
                origins = emptyList(),
                projectInfo = createTestProjectInfo(name = "error-project"),
                commitSha = "error-sha",
                createdAt = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            )

        val response =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody(errorResult)
            }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST to api-report with invalid JSON returns error`() = testApplication {
        application { module() }

        val response =
            client.post("/api/report") {
                contentType(ContentType.Application.Json)
                setBody("{invalid json}")
            }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST to api-report without content type fails`() = testApplication {
        application { module() }

        val response = client.post("/api/report") { setBody("{}") }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }
}
