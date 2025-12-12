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
import com.github.ajalt.clikt.parameters.options.required
import de.fraunhofer.iem.spha.adapter.ToolInfo
import de.fraunhofer.iem.spha.adapter.ToolResultParser
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.cli.SphaToolCommandBase
import de.fraunhofer.iem.spha.cli.network.GitHubProjectFetcher
import de.fraunhofer.iem.spha.cli.network.Language
import de.fraunhofer.iem.spha.cli.network.NetworkResponse
import de.fraunhofer.iem.spha.cli.network.ProjectInfo
import de.fraunhofer.iem.spha.core.KpiCalculator
import de.fraunhofer.iem.spha.model.adapter.Origin
import de.fraunhofer.iem.spha.model.kpi.hierarchy.DefaultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import java.nio.file.FileSystem
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
data class SphaToolResult(
    val resultHierarchy: KpiResultHierarchy,
    val origins: List<ToolInfoAndOrigin>,
    val projectInfo: ProjectInfo,
)

@Serializable data class ToolInfoAndOrigin(val toolInfo: ToolInfo, val origins: List<Origin>)

internal class AnalyzeRepositoryCommand :
    SphaToolCommandBase(
        name = "analyze",
        help =
            "Gathers project information from GitHub, transforms tool results into KPIs, and calculates the KPI Hierarchy.",
    ),
    KoinComponent {

    private val fileSystem by inject<FileSystem>()
    private val githubProjectFetcher = GitHubProjectFetcher()

    private val toolResultDir by
        option(
            "-t",
            "--toolResultDir",
            help =
                "The directory to read in JSON tool result files. Default is the current working directory.",
        )

    private val repoUrl by
        option(
                "-r",
                "--repoUrl",
                help =
                    "The project's repository URL. This is used to gather project information, such as the project's name and used technologies.",
            )
            .required()

    private val hierarchy by
        option(
            "-h",
            "--hierarchy",
            help =
                "Optional kpi hierarchy definition file. When not specified the default kpi hierarchy is used.",
        )

    private val output by option("-o", "--output", help = "The result file path.").required()

    override suspend fun run() {
        super.run()

        // Attempt to read GitHub token; proceed with defaults if unavailable
        val githubToken = System.getenv("GITHUB_TOKEN")
        if (githubToken.isNullOrBlank()) {
            Logger.error {
                "GITHUB_TOKEN environment variable not set. Proceeding without GitHub project metadata."
            }
            return
        }

        val projectInfoRes = githubProjectFetcher.use { it.getProjectInfo(repoUrl, githubToken) }
        val projectInfo =
            when (projectInfoRes) {
                is NetworkResponse.Success<ProjectInfo> -> projectInfoRes.data
                else -> defaultProjectInfo(repoUrl)
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
        writeResult(result)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeResult(result: SphaToolResult) {
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
}
