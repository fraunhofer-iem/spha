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
import de.fraunhofer.iem.spha.model.project.ProjectInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class ResultService(private val connection: Connection) {
    companion object {
        private const val CREATE_TABLE_PROJECTS =
            """CREATE TABLE IF NOT EXISTS PROJECTS (
                ID SERIAL PRIMARY KEY,
                NAME VARCHAR(255),
                URL VARCHAR(512) UNIQUE NOT NULL,
                STARS INT,
                NUMBER_OF_CONTRIBUTORS INT,
                NUMBER_OF_COMMITS INT,
                LAST_COMMIT_DATE VARCHAR(255)
            );"""

        private const val CREATE_TABLE_PROJECT_LANGUAGES =
            """CREATE TABLE IF NOT EXISTS PROJECT_LANGUAGES (
                ID SERIAL PRIMARY KEY,
                PROJECT_ID INT NOT NULL,
                LANGUAGE_NAME VARCHAR(255),
                SIZE INT,
                FOREIGN KEY (PROJECT_ID) REFERENCES PROJECTS(ID) ON DELETE CASCADE
            );"""

        private const val CREATE_TABLE_TOOL_RESULTS =
            """CREATE TABLE IF NOT EXISTS TOOL_RESULTS (
                ID SERIAL PRIMARY KEY,
                PROJECT_ID INT NOT NULL,
                RESULT_HIERARCHY TEXT NOT NULL,
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (PROJECT_ID) REFERENCES PROJECTS(ID) ON DELETE CASCADE
            );"""

        private const val CREATE_TABLE_TOOL_INFO_ORIGINS =
            """CREATE TABLE IF NOT EXISTS TOOL_INFO_ORIGINS (
                ID SERIAL PRIMARY KEY,
                RESULT_ID INT NOT NULL,
                TOOL_INFO TEXT NOT NULL,
                ORIGINS TEXT NOT NULL,
                FOREIGN KEY (RESULT_ID) REFERENCES TOOL_RESULTS(ID) ON DELETE CASCADE
            );"""

        private const val SELECT_PROJECT_BY_URL =
            "SELECT ID, NAME, URL, STARS, NUMBER_OF_CONTRIBUTORS, NUMBER_OF_COMMITS, LAST_COMMIT_DATE FROM PROJECTS WHERE URL = ?"
        private const val INSERT_PROJECT =
            "INSERT INTO PROJECTS (NAME, URL, STARS, NUMBER_OF_CONTRIBUTORS, NUMBER_OF_COMMITS, LAST_COMMIT_DATE) VALUES (?, ?, ?, ?, ?, ?)"
        private const val INSERT_LANGUAGE =
            "INSERT INTO PROJECT_LANGUAGES (PROJECT_ID, LANGUAGE_NAME, SIZE) VALUES (?, ?, ?)"
        private const val INSERT_RESULT =
            "INSERT INTO TOOL_RESULTS (PROJECT_ID, RESULT_HIERARCHY, CREATED_AT) VALUES (?, ?, ?)"
        private const val INSERT_TOOL_INFO_ORIGIN =
            "INSERT INTO TOOL_INFO_ORIGINS (RESULT_ID, TOOL_INFO, ORIGINS) VALUES (?, ?, ?)"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_PROJECTS)
        statement.executeUpdate(CREATE_TABLE_PROJECT_LANGUAGES)
        statement.executeUpdate(CREATE_TABLE_TOOL_RESULTS)
        statement.executeUpdate(CREATE_TABLE_TOOL_INFO_ORIGINS)
    }

    suspend fun findOrCreateProject(projectInfo: ProjectInfo): Int =
        withContext(Dispatchers.IO) {
            // Try to find existing project by URL
            val selectStmt = connection.prepareStatement(SELECT_PROJECT_BY_URL)
            selectStmt.setString(1, projectInfo.url)
            val resultSet = selectStmt.executeQuery()

            if (resultSet.next()) {
                return@withContext resultSet.getInt("ID")
            }

            // Project doesn't exist, create it
            val insertStmt =
                connection.prepareStatement(INSERT_PROJECT, Statement.RETURN_GENERATED_KEYS)
            insertStmt.setString(1, projectInfo.name)
            insertStmt.setString(2, projectInfo.url)
            insertStmt.setInt(3, projectInfo.stars)
            insertStmt.setInt(4, projectInfo.numberOfContributors)
            if (projectInfo.numberOfCommits != null) {
                insertStmt.setInt(5, projectInfo.numberOfCommits!!)
            } else {
                insertStmt.setNull(5, java.sql.Types.INTEGER)
            }
            if (projectInfo.lastCommitDate != null) {
                insertStmt.setString(6, projectInfo.lastCommitDate)
            } else {
                insertStmt.setNull(6, java.sql.Types.VARCHAR)
            }
            insertStmt.executeUpdate()

            val generatedKeys = insertStmt.generatedKeys
            if (!generatedKeys.next()) {
                throw Exception("Unable to retrieve the id of the newly inserted project")
            }
            val projectId = generatedKeys.getInt(1)

            // Insert languages
            val langStmt = connection.prepareStatement(INSERT_LANGUAGE)
            for (language in projectInfo.usedLanguages) {
                langStmt.setInt(1, projectId)
                langStmt.setString(2, language.name)
                langStmt.setInt(3, language.size)
                langStmt.executeUpdate()
            }

            return@withContext projectId
        }

    suspend fun createResult(projectId: Int, result: SphaToolResult): Int =
        withContext(Dispatchers.IO) {
            // Insert tool result
            val resultStmt =
                connection.prepareStatement(INSERT_RESULT, Statement.RETURN_GENERATED_KEYS)
            resultStmt.setInt(1, projectId)
            resultStmt.setString(2, Json.encodeToString(result.resultHierarchy))
            resultStmt.setTimestamp(3, Timestamp(System.currentTimeMillis()))
            resultStmt.executeUpdate()

            val generatedKeys = resultStmt.generatedKeys
            if (!generatedKeys.next()) {
                throw Exception("Unable to retrieve the id of the newly inserted result")
            }
            val resultId = generatedKeys.getInt(1)

            // Insert tool info and origins
            val toolInfoStmt = connection.prepareStatement(INSERT_TOOL_INFO_ORIGIN)
            for (toolInfoAndOrigin in result.origins) {
                toolInfoStmt.setInt(1, resultId)
                toolInfoStmt.setString(2, Json.encodeToString(toolInfoAndOrigin.toolInfo))
                toolInfoStmt.setString(3, Json.encodeToString(toolInfoAndOrigin.origins))
                toolInfoStmt.executeUpdate()
            }

            return@withContext resultId
        }
}

fun Application.configureDatabases() {
    val embedded =
        System.getenv("DATABASE_EMBEDDED")?.toBoolean()
            ?: environment.config.propertyOrNull("database.embedded")?.getString()?.toBoolean()
            ?: true
    log.info("Database embedded mode: $embedded")
    val dbConnection: Connection = connectToPostgres(embedded = embedded)
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
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the
 *   same process. In this case you don't have to provide any parameters in configuration file, and
 *   you don't have to run a process.
 * @return [Connection] that represent connection to the database. Please, don't forget to close
 *   this connection when your application shuts down by calling [Connection.close]
 */
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    if (embedded) {
        log.info("Using embedded H2 database for testing; replace this flag to use postgres")
        return DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url =
            System.getenv("POSTGRES_URL") ?: environment.config.property("postgres.url").getString()
        log.info("Connecting to postgres database at $url")
        val user =
            System.getenv("POSTGRES_USER")
                ?: environment.config.property("postgres.user").getString()
        val password =
            System.getenv("POSTGRES_PASSWORD")
                ?: environment.config.property("postgres.password").getString()

        return DriverManager.getConnection(url, user, password)
    }
}
