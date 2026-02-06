/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.reporting

import de.fraunhofer.iem.spha.model.SphaToolResult
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class HttpResultSenderTest {

    private fun loadTestResult(): SphaToolResult {
        // Search for the example file in likely locations
        val possiblePaths =
            listOf(
                Path.of("ui/example/kpi-results.json"),
                Path.of("../ui/example/kpi-results.json"),
                Path.of("cli/ui/example/kpi-results.json"),
                Path.of("../../ui/example/kpi-results.json"),
            )

        val jsonPath =
            possiblePaths.find { java.nio.file.Files.exists(it) }
                ?: throw java.io.FileNotFoundException(
                    "Could not find kpi-results.json in any of: $possiblePaths"
                )

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
        val receivedResult = CompletableDeferred<SphaToolResult>()

        val server =
            embeddedServer(Netty, port = 0) {
                    install(ContentNegotiation) { json() }
                    routing {
                        post("/report") {
                            val body = call.receive<SphaToolResult>()
                            receivedResult.complete(body)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                .start(wait = false)

        val port = server.engine.resolvedConnectors().first().port
        val url = "http://127.0.0.1:$port/report"

        try {
            sender.send(result, url)
            val captured = receivedResult.await()
            assertEquals(
                result,
                captured,
                "The received SphaToolResult should be identical to the sent one",
            )
        } finally {
            server.stop(100, 100)
        }
    }

    @Test
    fun `send throws exception for 500 error`(): Unit = runBlocking {
        val sender = HttpResultSender()
        val result = loadTestResult()

        val server =
            embeddedServer(Netty, port = 0) {
                    routing {
                        post("/report") {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Internal server error occurred",
                            )
                        }
                    }
                }
                .start(wait = false)

        val port = server.engine.resolvedConnectors().first().port
        val url = "http://127.0.0.1:$port/report"

        try {
            val exception = assertFailsWith<Exception> { sender.send(result, url) }
            assertContains(exception.message!!, "Server returned error status 500")
            assertContains(exception.message!!, "Internal server error occurred")
        } finally {
            server.stop(100, 100)
        }
    }

    @Test
    fun `send throws exception for unreachable server`(): Unit = runBlocking {
        val sender = HttpResultSender()
        val result = loadTestResult()

        assertFailsWith<Exception> { sender.send(result, "http://127.0.0.1:19999/nonexistent") }
    }

    @Test
    fun `send throws exception for invalid URL`(): Unit = runBlocking {
        val sender = HttpResultSender()
        val result = loadTestResult()

        assertFailsWith<Exception> { sender.send(result, "not-a-valid-url") }
    }

    @Test
    fun `send can load and serialize real test data`(): Unit = runBlocking {
        val result = loadTestResult()

        assertEquals("spha", result.projectInfo.name)
        assertEquals(10, result.projectInfo.stars)
        assertEquals(
            "https://github.com/fraunhofer-iem/spha",
            de.fraunhofer.iem.spha.cli.vcs.TestGitUtils.normalizeGitUrl(result.projectInfo.url),
        )
    }
}
