/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.commands

import com.github.ajalt.clikt.parameters.options.option
import de.fraunhofer.iem.spha.adapter.ToolResultParser
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.cli.SphaToolCommandBase
import de.fraunhofer.iem.spha.cli.reporting.HttpResultSender
import de.fraunhofer.iem.spha.cli.vcs.GitUtils
import de.fraunhofer.iem.spha.cli.vcs.NetworkResponse
import de.fraunhofer.iem.spha.cli.vcs.ProjectInfoFetcher
import de.fraunhofer.iem.spha.cli.vcs.ProjectInfoFetcherFactory
import de.fraunhofer.iem.spha.core.KpiCalculator
import de.fraunhofer.iem.spha.model.SphaToolResult
import de.fraunhofer.iem.spha.model.ToolInfoAndOrigin
import de.fraunhofer.iem.spha.model.kpi.hierarchy.DefaultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.project.Language
import de.fraunhofer.iem.spha.model.project.ProjectInfo
import java.nio.file.FileSystem
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class AnalyzeRepositoryCommand :
    SphaToolCommandBase(
        name = "analyze",
        help =
            "Gathers project information from GitHub, transforms tool results into KPIs, and calculates the KPI Hierarchy.",
    ),
    KoinComponent {

    private val fileSystem by inject<FileSystem>()

    private val toolResultDir by
        option(
            "-t",
            "--toolResultDir",
            help =
                "The directory to read in JSON tool result files. Default is the current working directory.",
        )

    private val repoOrigin by
        option(
            "-r",
            "--repoOrigin",
            help =
                "The project's repository URL. This is used to gather project information, such as the project's name and used technologies. If not specified, attempts to detect from the current git repository.",
        )

    private val repositoryType by
        option(
            "--repositoryType",
            help =
                "Override the auto-detected repository type. Valid values: github, gitlab, local. Use this for self-hosted instances.",
        )

    private val token by
        option(
            "--token",
            help =
                "Authentication token for the VCS platform. Overrides environment variables (GITHUB_TOKEN, GITLAB_TOKEN, etc.).",
        )

    private val hierarchy by
        option(
            "-h",
            "--hierarchy",
            help =
                "Optional kpi hierarchy definition file. When not specified the default kpi hierarchy is used.",
        )

    private val output by
        option(
            "-o",
            "--output",
            help = "The result file path. Required when --reportUri is not specified.",
        )

    private val reportUri by
        option(
            "--reportUri",
            help =
                "The server endpoint to POST the result as JSON (e.g., http://server:port/report). When specified, result is sent to server instead of writing to file.",
        )

    override suspend fun run() {
        super.run()

        if (output.isNullOrBlank() && reportUri.isNullOrBlank()) {
            Logger.error { "Either --output or --reportUri must be specified." }
            throw IllegalArgumentException("Either --output or --reportUri must be specified.")
        }

        // Determine repository URL or path
        val resolvedRepoOrigin = repoOrigin ?:
            GitUtils.detectGitRepositoryUrl() ?:
            "." // use current directory as fallback

        Logger.debug { "Using repository URL/path: $resolvedRepoOrigin" }

        val provider = getRepositoryFetcher(resolvedRepoOrigin, repositoryType)

        val projectInfoRes = provider.use { it.getProjectInfo(resolvedRepoOrigin, token) }
        val projectInfo =
            when (projectInfoRes) {
                is NetworkResponse.Success<ProjectInfo> -> projectInfoRes.data
                is NetworkResponse.Failed -> {
                    Logger.warn { "Failed to fetch project info: ${projectInfoRes.msg}" }
                    defaultProjectInfo(resolvedRepoOrigin)
                }
            }

        Logger.info { "Project info: $projectInfo" }

        // Determine tool results directory
        val toolPath =
            (this.toolResultDir?.takeIf { it.isNotBlank() } ?: ".").let {
                fileSystem.getPath(it).toAbsolutePath().toString()
            }
        Logger.debug { "Reading tool results from: $toolPath" }

        val adapterResults = ToolResultParser.parseJsonFilesFromDirectory(directoryPath = toolPath)
        Logger.info { "Parsed ${adapterResults.size} tool result file(s)." }

        if (adapterResults.isEmpty()) {
            Logger.warn { "No KPI values to calculate: adapter results are empty." }
        }

        val rawValueKpis =
            adapterResults.flatMap { result ->
                if (result.transformationResults.isNotEmpty()) {
                    result.transformationResults.mapNotNull {
                        if (it is TransformationResult.Success<*>) it.rawValueKpi else null
                    }
                } else emptyList()
            }
        Logger.info { "Collected ${rawValueKpis.size} raw KPI value(s)." }

        val hierarchyModel = getHierarchy()
        val kpiResult = KpiCalculator.calculateKpis(hierarchyModel, rawValueKpis)

        val originsData =
            adapterResults.mapNotNull { result ->
                result.toolInfo?.let { toolInfo ->
                    val origins =
                        result.transformationResults.mapNotNull {
                            if (it is TransformationResult.Success<*>) it.origin else null
                        }
                    ToolInfoAndOrigin(toolInfo, origins)
                }
            }

        val result = SphaToolResult(kpiResult, originsData, projectInfo)
        processResult(result)
    }

    private suspend fun processResult(result: SphaToolResult) {
        val output = this.output
        val reportUri = this.reportUri

        if (!reportUri.isNullOrBlank()) {
            try {
                HttpResultSender().send(result, reportUri)
            } catch (e: Exception) {
                Logger.error(e) { "Failed to send result to $reportUri: ${e.message}" }
                throw e
            }
        }
        if (!output.isNullOrBlank()) {
            writeToFile(result, output)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeToFile(result: SphaToolResult, output: String) {
        val outputFilePath = fileSystem.getPath(output)

        val directory = outputFilePath.toAbsolutePath().parent
        directory?.createDirectories()

        Logger.info { "Writing result to: ${outputFilePath.toAbsolutePath()}" }
        outputFilePath.outputStream().use { Json.encodeToStream(result, it) }
    }

    private fun defaultProjectInfo(repoUrl: String) =
        ProjectInfo(
            name = "Currently no data available",
            usedLanguages = listOf(Language("Currently no data available", 100)),
            url = repoUrl,
            numberOfContributors = -1,
            numberOfCommits = -1,
            lastCommitDate = "Currently no data available",
            stars = -1,
        )

    @OptIn(ExperimentalSerializationApi::class)
    private fun getHierarchy(): KpiHierarchy {
        if (hierarchy.isNullOrBlank()) return DefaultHierarchy.get()

        return try {
            fileSystem.getPath(hierarchy!!).inputStream().use {
                Json.decodeFromStream<KpiHierarchy>(it)
            }
        } catch (e: Exception) {
            Logger.error(e) {
                "Failed to read or parse hierarchy from '$hierarchy'. Falling back to default hierarchy."
            }
            DefaultHierarchy.get()
        }
    }

    private fun getRepositoryFetcher(repoUrl: String, repositoryType: String?): ProjectInfoFetcher {
        return if (repositoryType != null) {
            try {
                ProjectInfoFetcherFactory.createFetcher(repositoryType)
            } catch (e: IllegalArgumentException) {
                Logger.error { e.message }
                throw e
            }
        } else {
            ProjectInfoFetcherFactory.createFetcherFromUrl(repoUrl)
        }
    }
}
