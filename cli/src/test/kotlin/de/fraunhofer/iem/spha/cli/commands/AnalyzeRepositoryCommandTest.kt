/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.commands

import com.github.ajalt.clikt.command.test
import de.fraunhofer.iem.spha.cli.appModules
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
import io.mockk.mockkClass
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension

class AnalyzeRepositoryCommandTest : KoinTest {

    companion object {
        private const val OUTPUT_FILE = "output.json"
        private const val SAMPLE_RESULT_DIR = "../ui/example"
        private const val CURRENT_REPO_LOCAL = ".."

        private const val NAME_NOT_FOUND_MESSAGE = "Currently no data available"

        private val isOnlineTest = (System.getenv("GITHUB_TOKEN")
            ?: System.getenv("GH_TOKEN")) != null

        @JvmStatic
        fun commandTestCases() = listOf(
            Arguments.of(
                "Command with local path and local reposotryType succeeds with local repository",
                "--repoOrigin \"$CURRENT_REPO_LOCAL\" --repositoryType local",
                Path(CURRENT_REPO_LOCAL).toRealPath().toString(),
                true
            ),
            Arguments.of(
                "Command auto-detects git repository and uses online data when neither repoOrigin nor repositoryType is specified",
                "",
                if (isOnlineTest) "spha" else NAME_NOT_FOUND_MESSAGE,
                isOnlineTest
            ),
            Arguments.of(
                "Command with local repoOrigin and no repositoryType always uses local data",
                "--repoOrigin \"$CURRENT_REPO_LOCAL\"",
                Path(CURRENT_REPO_LOCAL).toRealPath().toString(),
                true
            ),
              Arguments.of(
                "Command with remote repoOrigin and no repositoryType resolves online data",
                "--repoOrigin \"https://github.com/fraunhofer-iem/spha\"",
                  if (isOnlineTest) "spha" else NAME_NOT_FOUND_MESSAGE,
                  isOnlineTest
              ),
            Arguments.of(
                "Command with remote repoOrigin and no correct repositoryType resolves online data",
                "--repoOrigin \"https://github.com/fraunhofer-iem/spha\" --repositoryType github",
                  if (isOnlineTest) "spha" else NAME_NOT_FOUND_MESSAGE,
                  isOnlineTest
              )
        )
    }

    @JvmField
    @RegisterExtension
    val koinTestRule =
        KoinTestExtension.create {
            printLogger(Level.DEBUG)
            modules(appModules)
        }

    @AfterTest
    fun cleanup() {
        Files.deleteIfExists(Path(OUTPUT_FILE))
    }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz -> mockkClass(clazz) }

    @Test
    fun `command requires either output or reportUri`() = runTest {
        val command = AnalyzeRepositoryCommand()
        val e = assertThrows<IllegalArgumentException> { command.test("") }
        assertEquals("Either --output or --reportUri must be specified.", e.message)
    }

    @Test
    fun `command fails with invalid repository type`() = runTest {
        val command = AnalyzeRepositoryCommand()
        val e =
            assertThrows<IllegalArgumentException> {
                command.test("--output \"${OUTPUT_FILE}\" --repositoryType invalid-type")
            }
        assertContains(e.message!!, "Invalid repository type:")
    }

    @OptIn(ExperimentalSerializationApi::class)
    @ParameterizedTest(name = "{0}")
    @MethodSource("commandTestCases")
    fun `command executes successfully with various arguments`(
        description: String,
        variableArgs: String,
        expectedName: String,
        shouldCheckKotlinLanguage: Boolean
    ) = runTest {
        val command = AnalyzeRepositoryCommand()
        val result = command.test(
            "$variableArgs --output \"$OUTPUT_FILE\" --toolResultDir \"$SAMPLE_RESULT_DIR\""
        )

        assertEquals(
            0,
            result.statusCode,
            "$description. Output: ${result.stdout}\n${result.stderr}",
        )
        assertTrue(Files.exists(Path(OUTPUT_FILE)), "Output file should be created at $OUTPUT_FILE")

        Files.newInputStream(Path(OUTPUT_FILE)).use { inputStream ->
            val sphaResult = Json.decodeFromStream<SphaToolResult>(inputStream)
            assertNotNull(sphaResult.projectInfo, "Project info should not be null")

            assertEquals(expectedName, sphaResult.projectInfo.name)
            assertEquals("https://github.com/fraunhofer-iem/spha", sphaResult.projectInfo.url)

            if (shouldCheckKotlinLanguage) {
                assertTrue(
                    sphaResult.projectInfo.usedLanguages.any { it.name == "Kotlin" },
                    "Should detect Kotlin language",
                )
            }
        }
    }


    @Test
    fun `command respects token override`() = runTest {
        val command = AnalyzeRepositoryCommand()
        val result =
            command.test(
                "--repoOrigin \"$CURRENT_REPO_LOCAL\" --output \"$OUTPUT_FILE\" --repositoryType local --token test-token-123 --toolResultDir \"$SAMPLE_RESULT_DIR\""
            )

        // Should succeed - local repos don't need tokens anyway
        assertEquals(0, result.statusCode)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `command fails when invalid token supplied for github`() = runTest {
        val repoUrl = "https://github.com/fraunhofer-iem/spha"
        val command = AnalyzeRepositoryCommand()

        val result =
            command.test(
                "--repoOrigin \"$repoUrl\" --output \"$OUTPUT_FILE\" --repositoryType github --token invalid-token-xyz"
            )

        // When GitHub fetching fails, it logs a warning and uses default info.
        // It does NOT return a non-zero exit code currently in the implementation.
        // However, it should have logged the error.
        assertEquals(0, result.statusCode)

        // Verify that the output file contains the default "Currently no data available" info
        assertTrue(Files.exists(Path(OUTPUT_FILE)))
        Files.newInputStream(Path(OUTPUT_FILE)).use { inputStream ->
            val sphaResult = Json.decodeFromStream<SphaToolResult>(inputStream)
            assertEquals("Currently no data available", sphaResult.projectInfo.name)
        }
    }

    @Test
    fun `command can send to reportUri instead of file`() = runTest {
        val repoDir = ".."
        val toolResultDir = "../ui/example"
        val receivedResult = CompletableDeferred<SphaToolResult>()

        val server =
            embeddedServer(Netty, port = 0) {
                    install(ContentNegotiation) { json() }
                    routing {
                        post("/report") {
                            val result = call.receive<SphaToolResult>()
                            receivedResult.complete(result)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                .start(wait = false)

        val port = server.engine.resolvedConnectors().first().port
        val reportUri = "http://127.0.0.1:$port/report"

        try {
            val command = AnalyzeRepositoryCommand()
            val result =
                command.test(
                    "--repoOrigin \"$repoDir\" --reportUri $reportUri --repositoryType local --toolResultDir \"$toolResultDir\""
                )

            assertEquals(
                0,
                result.statusCode,
                "Command should succeed sending to reportUri. Output: ${result.stdout}\n${result.stderr}",
            )

            val sphaResult = receivedResult.await()
            assertNotNull(sphaResult.projectInfo, "Project info should not be null")
            assertEquals("https://github.com/fraunhofer-iem/spha", sphaResult.projectInfo.url)
            assertEquals("..", sphaResult.projectInfo.name)
            assertTrue(sphaResult.projectInfo.usedLanguages.any { it.name == "Kotlin" }, "Should detect Kotlin language")
            // origins might be empty if no tool results were parsed
            assertNotNull(sphaResult.resultHierarchy.root, "KPI result root should not be null")
        } finally {
            server.stop(100, 100)
        }
    }

    @Test
    fun `command fails when reportUri returns 500 error`() = runTest {
        val repoDir = ".."
        val toolResultDir = "../ui/example"

        val server =
            embeddedServer(Netty, port = 0) {
                    routing {
                        post("/report") {
                            call.respond(HttpStatusCode.InternalServerError, "Server error")
                        }
                    }
                }
                .start(wait = false)

        val port = server.engine.resolvedConnectors().first().port
        val reportUri = "http://127.0.0.1:$port/report"

        try {
            val command = AnalyzeRepositoryCommand()
            val exception =
                assertThrows<Exception> {
                    command.test(
                        "--repoOrigin \"$repoDir\" --reportUri $reportUri --repositoryType local --toolResultDir \"$toolResultDir\""
                    )
                }
            assertContains(exception.message!!, "Server returned error status 500")
        } finally {
            server.stop(100, 100)
        }
    }
}
