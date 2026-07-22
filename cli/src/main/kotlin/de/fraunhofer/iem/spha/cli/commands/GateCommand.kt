/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.commands

import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.fraunhofer.iem.spha.adapter.ToolResultParser
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.cli.GATE_EXIT_CODE
import de.fraunhofer.iem.spha.cli.QualityGate
import de.fraunhofer.iem.spha.cli.SphaToolCommandBase
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.DefaultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import java.nio.file.FileSystem
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.walk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A first-class, fail-closed CI quality gate. It runs the same compute path as `analyze` /
 * `calculate`, then inspects the typed
 * [de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult] of the designated blocking
 * node(s) and exits non-zero unless every one of them is a `Success` at or above `--min-score`.
 * Anything else — `Error`, `Incomplete`, `Empty`, a missing node, or a low score — fails the build
 * ([GATE_EXIT_CODE]).
 *
 * This removes the need for an external shell/`jq` wrapper over the serialized hierarchy; see
 * [QualityGate] for the evaluation contract.
 */
internal class GateCommand :
    SphaToolCommandBase(
        name = "gate",
        help =
            "Computes the KPI hierarchy and exits non-zero unless every required blocking node " +
                "passes. Fail-closed: anything that is not Success at/above --min-score — Error, " +
                "Incomplete, Empty, a missing node, or a low score — fails the gate.",
    ),
    KoinComponent {

    private val fileSystem by inject<FileSystem>()
    private val kpiCalculatorService by inject<KpiCalculatorService>()

    private val toolResultDir by
        option(
            "-t",
            "--toolResultDir",
            help =
                "Directory of JSON tool result files (adapter inputs, like `analyze`). Combined with --sourceDir when both are given.",
        )

    private val sourceDir by
        option(
            "-s",
            "--sourceDir",
            help =
                "Directory of JSON raw KPI value files (like `calculate`). Combined with --toolResultDir when both are given.",
        )

    private val hierarchy by
        option(
            "-h",
            "--hierarchy",
            help =
                "Optional KPI hierarchy definition file. When not specified the default KPI hierarchy is used.",
        )

    private val requireNodes by
        option(
                "--require-node",
                help =
                    "typeId of a node that must pass. Repeatable. Defaults to the hierarchy root when omitted.",
            )
            .multiple()

    private val minScore by
        option("--min-score", help = "Minimum passing score for each required node. Default 100.")
            .int()
            .default(100)

    private val output by
        option(
            "-o",
            "--output",
            help =
                "Optional path to also write the computed KPI hierarchy JSON (e.g. as a CI artifact).",
        )

    override suspend fun run() {
        super.run()

        if (toolResultDir.isNullOrBlank() && sourceDir.isNullOrBlank()) {
            Logger.error { "Specify at least one of --toolResultDir (-t) or --sourceDir (-s)." }
            throw IllegalArgumentException(
                "Specify at least one of --toolResultDir (-t) or --sourceDir (-s)."
            )
        }

        val rawValueKpis = collectRawValueKpis()
        if (rawValueKpis.isEmpty()) {
            Logger.warn { "No KPI values collected; the gate will evaluate an empty hierarchy." }
        }

        val hierarchyModel = getHierarchy()
        val kpiResult = kpiCalculatorService.calculateKpis(hierarchyModel, rawValueKpis)

        output?.takeIf { it.isNotBlank() }?.let { writeHierarchy(kpiResult, it) }

        val evaluation = QualityGate.evaluate(kpiResult, requireNodes, minScore)
        evaluation.verdicts.forEach { verdict ->
            val marker = if (verdict.passed) "PASS" else "FAIL"
            echo("$marker  ${verdict.typeId}: ${verdict.detail}", err = !verdict.passed)
        }
        echo("----------------------------------------")

        if (evaluation.passed) {
            echo("Gate PASSED — all required blocking KPIs pass (min-score=$minScore).")
        } else {
            echo(
                "Gate FAILED (fail-closed) — at least one required blocking KPI did not pass.",
                err = true,
            )
            throw ProgramResult(GATE_EXIT_CODE)
        }
    }

    private fun collectRawValueKpis(): List<RawValueKpi> {
        val kpis = mutableListOf<RawValueKpi>()

        toolResultDir
            ?.takeIf { it.isNotBlank() }
            ?.let { dir ->
                val toolPath = fileSystem.getPath(dir).toAbsolutePath().toString()
                Logger.debug { "Reading tool results from: $toolPath" }
                val adapterResults = ToolResultParser.parseJsonFilesFromDirectory(toolPath)
                Logger.info { "Parsed ${adapterResults.size} tool result file(s)." }
                kpis +=
                    adapterResults.flatMap { result ->
                        result.transformationResults.mapNotNull {
                            if (it is TransformationResult.Success<*>) it.rawValueKpi else null
                        }
                    }
            }

        sourceDir
            ?.takeIf { it.isNotBlank() }
            ?.let { dir ->
                Logger.debug { "Reading raw KPI values from: $dir" }
                kpis += readRawValueKpis(dir)
            }

        Logger.info { "Collected ${kpis.size} raw KPI value(s)." }
        return kpis
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    private fun readRawValueKpis(dir: String): List<RawValueKpi> {
        val location = fileSystem.getPath(dir)
        val result = mutableListOf<RawValueKpi>()
        for (file in location.walk().sorted()) {
            if (!file.extension.equals("json", true)) continue
            file.inputStream().use {
                try {
                    result.addAll(Json.decodeFromStream<Collection<RawValueKpi>>(it))
                    Logger.trace { "read kpi file '${file.absolutePathString()}'." }
                } catch (_: SerializationException) {
                    Logger.trace {
                        "could not deserialize '${file.absolutePathString()}' to a collection of raw kpi values."
                    }
                }
            }
        }
        return result
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeHierarchy(kpiResult: KpiResultHierarchy, output: String) {
        val outputFilePath = fileSystem.getPath(output)
        outputFilePath.toAbsolutePath().parent?.createDirectories()
        Logger.info { "Writing computed hierarchy to: ${outputFilePath.toAbsolutePath()}" }
        outputFilePath.outputStream().use { Json.encodeToStream(kpiResult, it) }
    }

    // Fail-closed: unlike `analyze`, a bad hierarchy file is NOT silently replaced by the default —
    // it throws, so the gate fails rather than gating on an unintended hierarchy.
    @OptIn(ExperimentalSerializationApi::class)
    private fun getHierarchy(): KpiHierarchy {
        if (hierarchy.isNullOrBlank()) return DefaultHierarchy.get()
        fileSystem.getPath(hierarchy!!).inputStream().use {
            return Json.decodeFromStream<KpiHierarchy>(it)
        }
    }
}
