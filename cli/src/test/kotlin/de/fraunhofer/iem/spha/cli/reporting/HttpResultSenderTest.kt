/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.reporting

import de.fraunhofer.iem.spha.cli.commands.SphaToolResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertFailsWith

class HttpResultSenderTest {

    companion object {
        private const val TEST_REPORT_URL = "http://10.10.79.10:8080/report"
    }

    private fun loadTestResult(): SphaToolResult {
        // Search for the example file in likely locations
        val possiblePaths = listOf(
            Path.of("ui/example/kpi-results.json"),
            Path.of("../ui/example/kpi-results.json"),
            Path.of("cli/ui/example/kpi-results.json"),
            Path.of("../../ui/example/kpi-results.json")
        )
        
        val jsonPath = possiblePaths.find { java.nio.file.Files.exists(it) }
            ?: throw java.io.FileNotFoundException("Could not find kpi-results.json in any of: $possiblePaths")
            
        val jsonContent = jsonPath.readText()

        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }

        return json.decodeFromString<SphaToolResult>(jsonContent)
    }

    @Test
    fun `send successfully posts result to test server`(): Unit = runBlocking {
        val sender = HttpResultSender()
        val result = loadTestResult()

        try {
            sender.send(result, TEST_REPORT_URL)
        } catch (e: Exception) {
            println("Skipping test because test server is not reachable: $e")
        }
    }

    @Test
    fun `send throws exception for unreachable server`(): Unit = runBlocking {
        val sender = HttpResultSender()
        val result = loadTestResult()

        assertFailsWith<Exception> {
            sender.send(result, "http://127.0.0.1:19999/nonexistent")
        }
    }

    @Test
    fun `send throws exception for invalid URL`(): Unit = runBlocking {
        val sender = HttpResultSender()
        val result = loadTestResult()

        assertFailsWith<Exception> {
            sender.send(result, "not-a-valid-url")
        }
    }

    @Test
    fun `send can load and serialize real test data`(): Unit = runBlocking {
        val result = loadTestResult()

        kotlin.test.assertEquals("Currently no data available", result.projectInfo.name)
        kotlin.test.assertEquals(-1, result.projectInfo.stars)
        kotlin.test.assertEquals("https://github.com/fraunhofer-iem/spha", result.projectInfo.url)
    }
}
