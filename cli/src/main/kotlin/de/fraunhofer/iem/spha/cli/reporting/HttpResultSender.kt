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
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** A class responsible for sending analysis results to a remote server via HTTP. */
class HttpResultSender {
    private val logger = KotlinLogging.logger {}

    /**
     * Sends the analysis result to the specified server URI via HTTP POST.
     *
     * @param result The analysis result to send
     * @param uri The server URI to send the result to
     * @throws Exception if the HTTP request fails
     */
    suspend fun send(result: SphaToolResult, uri: String) {
        logger.info { "Sending result to server: $uri" }

        val client =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = false
                            ignoreUnknownKeys = true
                        }
                    )
                }
            }

        try {
            val response =
                client.post(uri) {
                    contentType(ContentType.Application.Json)
                    setBody(result)
                }

            val responseBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                val errorMessage =
                    "Server returned error status ${response.status.value}: $responseBody"
                logger.error { errorMessage }
                throw Exception(errorMessage)
            }

            logger.info { "Server response: ${response.status}" }
            logger.debug { "Response body: $responseBody" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send result to server: ${e.message}" }
            throw e
        } finally {
            client.close()
        }
    }
}
