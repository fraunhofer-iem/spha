/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.database

import de.fraunhofer.iem.spha.model.SphaToolResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection

fun Application.configureDatabases() {
    val dbConnection: Connection = connectToPostgres()
    val resultService = ResultService(dbConnection)

    routing {
        // Submit new SPHA tool result
        post("/api/report") {
            try {
                val result = call.receive<SphaToolResult>()
                val projectId = resultService.findOrCreateProject(result.projectInfo)
                val resultId = resultService.createResult(projectId, result)
                call.respond(
                    HttpStatusCode.Created,
                    mapOf("id" to resultId, "projectId" to projectId),
                )
            } catch (e: io.ktor.server.plugins.BadRequestException) {
                log.error("Error storing result", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request body")),
                )
            } catch (e: io.ktor.server.plugins.CannotTransformContentToTypeException) {
                log.error("Error storing result", e)
                call.respond(
                    HttpStatusCode.UnsupportedMediaType,
                    mapOf("error" to (e.message ?: "Unsupported media type")),
                )
            } catch (e: Exception) {
                log.error("Error storing result", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error")),
                )
            }
        }

        // Get all project IDs
        get("/api/projects") {
            try {
                val projectIds = resultService.getAllProjectIds()
                call.respond(HttpStatusCode.OK, mapOf("projectIds" to projectIds))
            } catch (e: Exception) {
                log.error("Error retrieving project IDs", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error")),
                )
            }
        }

        // Get all results for a specific project
        get("/api/projects/{projectId}/results") {
            try {
                val projectId =
                    call.parameters["projectId"]?.toIntOrNull()
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid project ID"),
                        )
                val results = resultService.getResultsByProjectId(projectId)
                call.respond(HttpStatusCode.OK, mapOf("results" to results))
            } catch (e: Exception) {
                log.error("Error retrieving results for project", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error")),
                )
            }
        }
    }
}

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process, please specify the following parameters in
 * your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to
 * [download]((https://www.postgresql.org/download/)) and install Postgres and follow the
 * instructions [here](https://postgresapp.com/). Then, you would be able to edit your url, which is
 * usually "jdbc:postgresql://host:port/database", as well as user and password values.
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close
 *   this connection when your application shuts down by calling [Connection.close]
 */
fun Application.connectToPostgres(): Connection {
    Class.forName("org.postgresql.Driver")
    val url =
        System.getenv("POSTGRES_URL")
            ?: System.getProperty("POSTGRES_URL")
            ?: environment.config.property("postgres.url").getString()
    log.info("Connecting to postgres database at $url")
    val user =
        System.getenv("POSTGRES_USER")
            ?: System.getProperty("POSTGRES_USER")
            ?: environment.config.property("postgres.user").getString()
    val password =
        System.getenv("POSTGRES_PASSWORD")
            ?: System.getProperty("POSTGRES_PASSWORD")
            ?: environment.config.property("postgres.password").getString()

    return java.sql.DriverManager.getConnection(url, user, password)
}
