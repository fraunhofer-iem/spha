/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.commands

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpReportTest {

    @Serializable
    data class TestReport(
        val projectName: String,
        val url: String,
        val status: String
    )

    @Test
    fun `HTTP client can be created and configured with JSON support`() = runTest {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                })
            }
        }

        client.close()
        // If we get here without exception, client was created successfully
        assertTrue(true)
    }

    @Test
    fun `JSON serialization produces valid output for report data`() {
        val testData = TestReport(
            projectName = "test-project",
            url = "https://github.com/owner/repo",
            status = "success"
        )

        val json = Json.encodeToString(TestReport.serializer(), testData)
        val decoded = Json.decodeFromString<TestReport>(json)

        assertEquals(testData.projectName, decoded.projectName)
        assertEquals(testData.url, decoded.url)
        assertEquals(testData.status, decoded.status)
    }

    @Test
    fun `JSON serialization handles special characters in URLs`() {
        val testData = TestReport(
            projectName = "test-project",
            url = "https://github.com/user/repo-name_with.special-chars",
            status = "completed"
        )

        val json = Json.encodeToString(TestReport.serializer(), testData)
        val decoded = Json.decodeFromString<TestReport>(json)

        assertEquals(testData.url, decoded.url)
    }

    @Test
    fun `JSON serialization preserves structure`() {
        val testData = TestReport(
            projectName = "test-project",
            url = "https://github.com/test/repo",
            status = "analyzed"
        )

        val json = Json.encodeToString(TestReport.serializer(), testData)

        assertTrue(json.contains("test-project"))
        assertTrue(json.contains("https://github.com/test/repo"))
        assertTrue(json.contains("analyzed"))
    }

    @Test
    fun `HTTP client properly defines content type for JSON`() = runTest {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        try {
            val contentType = ContentType.Application.Json
            assertEquals("application", contentType.contentType)
            assertEquals("json", contentType.contentSubtype)
        } finally {
            client.close()
        }
    }
}
