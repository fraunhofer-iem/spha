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
import de.fraunhofer.iem.spha.model.ToolInfoAndOrigin
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.project.ProjectInfo
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.Logger

class ResultService(private val connection: Connection, private val log: Logger) {
    companion object {
        private const val CREATE_TABLE_PROJECTS =
            """CREATE TABLE IF NOT EXISTS PROJECTS (
                ID SERIAL PRIMARY KEY,
                NAME VARCHAR(255) NOT NULL,
                URL VARCHAR(512) UNIQUE NOT NULL
            );"""

        private const val CREATE_TABLE_TOOL_RESULTS =
            """CREATE TABLE IF NOT EXISTS TOOL_RESULTS (
                ID SERIAL PRIMARY KEY,
                PROJECT_ID INT NOT NULL,
                RESULT_HIERARCHY JSONB NOT NULL,
                REPOSITORY_INFO JSONB NOT NULL,
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (PROJECT_ID) REFERENCES PROJECTS(ID) ON DELETE CASCADE
            );"""

        private const val CREATE_TABLE_TOOL_INFO_ORIGINS =
            """CREATE TABLE IF NOT EXISTS TOOL_INFO_ORIGINS (
                ID SERIAL PRIMARY KEY,
                RESULT_ID INT NOT NULL,
                TOOL_INFO JSONB NOT NULL,
                ORIGINS JSONB NOT NULL,
                FOREIGN KEY (RESULT_ID) REFERENCES TOOL_RESULTS(ID) ON DELETE CASCADE
            );"""

        private const val SELECT_PROJECT_BY_URL = "SELECT ID, NAME, URL FROM PROJECTS WHERE URL = ?"
        private const val INSERT_PROJECT = "INSERT INTO PROJECTS (NAME, URL) VALUES (?, ?)"
        private const val INSERT_RESULT =
            "INSERT INTO TOOL_RESULTS (PROJECT_ID, RESULT_HIERARCHY, REPOSITORY_INFO, CREATED_AT) VALUES (?, ?, ?, ?)"
        private const val INSERT_TOOL_INFO_ORIGIN =
            "INSERT INTO TOOL_INFO_ORIGINS (RESULT_ID, TOOL_INFO, ORIGINS) VALUES (?, ?, ?)"
        private const val SELECT_ALL_PROJECT_IDS = "SELECT ID FROM PROJECTS ORDER BY ID"
        private const val SELECT_PROJECT_BY_ID = "SELECT ID, NAME, URL FROM PROJECTS WHERE ID = ?"
        private const val SELECT_RESULTS_BY_PROJECT_ID =
            "SELECT ID, RESULT_HIERARCHY, REPOSITORY_INFO, CREATED_AT FROM TOOL_RESULTS WHERE PROJECT_ID = ? ORDER BY CREATED_AT DESC"
        private const val SELECT_TOOL_INFO_ORIGINS_BY_RESULT_ID =
            "SELECT TOOL_INFO, ORIGINS FROM TOOL_INFO_ORIGINS WHERE RESULT_ID = ?"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_PROJECTS)
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

            // Project doesn't exist, create it (only name and URL)
            val insertStmt =
                connection.prepareStatement(INSERT_PROJECT, Statement.RETURN_GENERATED_KEYS)
            insertStmt.setString(1, projectInfo.name)
            insertStmt.setString(2, projectInfo.url)
            insertStmt.executeUpdate()

            val generatedKeys = insertStmt.generatedKeys
            if (!generatedKeys.next()) {
                throw Exception("Unable to retrieve the id of the newly inserted project")
            }
            return@withContext generatedKeys.getInt(1)
        }

    suspend fun createResult(projectId: Int, result: SphaToolResult): Int =
        withContext(Dispatchers.IO) {
            // Insert tool result with repository info
            val resultStmt =
                connection.prepareStatement(INSERT_RESULT, Statement.RETURN_GENERATED_KEYS)
            resultStmt.setInt(1, projectId)

            // Serialize hierarchy and repository info as JSONB
            val hierarchyJson = Json.encodeToString(result.resultHierarchy)
            resultStmt.setObject(
                2,
                org.postgresql.util.PGobject().apply {
                    type = "jsonb"
                    value = hierarchyJson
                },
            )

            val repositoryInfoJson = Json.encodeToString(result.projectInfo)
            resultStmt.setObject(
                3,
                org.postgresql.util.PGobject().apply {
                    type = "jsonb"
                    value = repositoryInfoJson
                },
            )

            resultStmt.setTimestamp(4, Timestamp(result.createdAt.toEpochMilliseconds()))
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
                val toolInfoJson = Json.encodeToString(toolInfoAndOrigin.toolInfo)
                val originsJson = Json.encodeToString(toolInfoAndOrigin.origins)

                // For JSONB columns, use PGobject
                toolInfoStmt.setObject(
                    2,
                    org.postgresql.util.PGobject().apply {
                        type = "jsonb"
                        value = toolInfoJson
                    },
                )
                toolInfoStmt.setObject(
                    3,
                    org.postgresql.util.PGobject().apply {
                        type = "jsonb"
                        value = originsJson
                    },
                )
                toolInfoStmt.executeUpdate()
            }

            return@withContext resultId
        }

    suspend fun getAllProjectIds(): List<Int> =
        withContext(Dispatchers.IO) {
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(SELECT_ALL_PROJECT_IDS)
            val projectIds = mutableListOf<Int>()
            while (resultSet.next()) {
                projectIds.add(resultSet.getInt("ID"))
            }
            log.debug("Fetched ${projectIds.size} project IDs from database")
            return@withContext projectIds
        }

    suspend fun getResultsByProjectId(projectId: Int): List<SphaToolResult> =
        withContext(Dispatchers.IO) {
            // Get project info (only name and URL)
            val projectStmt = connection.prepareStatement(SELECT_PROJECT_BY_ID)
            projectStmt.setInt(1, projectId)
            val projectResult = projectStmt.executeQuery()
            if (!projectResult.next()) {
                return@withContext emptyList()
            }

            // Get all results for this project
            val resultsStmt = connection.prepareStatement(SELECT_RESULTS_BY_PROJECT_ID)
            resultsStmt.setInt(1, projectId)
            val resultsSet = resultsStmt.executeQuery()

            val sphaResults = mutableListOf<SphaToolResult>()
            while (resultsSet.next()) {
                val resultId = resultsSet.getInt("ID")
                val resultHierarchyJson = resultsSet.getString("RESULT_HIERARCHY")
                val repositoryInfoJson = resultsSet.getString("REPOSITORY_INFO")
                val createdAtTimestamp = resultsSet.getTimestamp("CREATED_AT")

                // Get tool info and origins for this result
                val toolInfoStmt =
                    connection.prepareStatement(SELECT_TOOL_INFO_ORIGINS_BY_RESULT_ID)
                toolInfoStmt.setInt(1, resultId)
                val toolInfoSet = toolInfoStmt.executeQuery()

                val origins = mutableListOf<ToolInfoAndOrigin>()
                while (toolInfoSet.next()) {
                    val toolInfoJson = toolInfoSet.getString("TOOL_INFO")
                    val originsJson = toolInfoSet.getString("ORIGINS")
                    origins.add(
                        Json.decodeFromString<ToolInfoAndOrigin>(
                            """{"toolInfo":$toolInfoJson,"origins":$originsJson}"""
                        )
                    )
                }

                val resultHierarchy = Json.decodeFromString<KpiResultHierarchy>(resultHierarchyJson)
                val projectInfo = Json.decodeFromString<ProjectInfo>(repositoryInfoJson)

                sphaResults.add(
                    SphaToolResult(
                        resultHierarchy = resultHierarchy,
                        origins = origins,
                        projectInfo = projectInfo,
                        createdAt =
                            kotlin.time.Instant.fromEpochMilliseconds(createdAtTimestamp.time),
                    )
                )
            }

            return@withContext sphaResults
        }
}
