/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.commands

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.fraunhofer.iem.spha.cli.SphaToolCommandBase
import de.fraunhofer.iem.spha.core.KpiCalculator
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.DefaultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import java.nio.file.FileSystem
import java.nio.file.Path
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

internal class CalculateKpiCommand :
    SphaToolCommandBase(
        name = "calculate",
        help = "Builds a KPI hierarchy from raw values and a hierarchy definition.",
    ),
    KoinComponent {

    private val fileSystem by inject<FileSystem>()

    private val sourceDir by
        option(
            "-s",
            "--sourceDir",
            help =
                "The directory to read in JSON raw kpi value files. Default is the current working directory.",
        )

    private val hierarchy by
        option(
            "-h",
            "--hierarchy",
            help =
                "Optional kpi hierarchy definition file. When not specified the default kpi hierarchy is used.",
        )

    private val output by
        option("-o", "--output", help = "The file to which the KPI hierarchy shall get written to.")
            .required()

    override suspend fun run() {
        super.run()
        val rawValueKpis = getKpiValuesFromSource()

        if (rawValueKpis.isEmpty()) {
            Logger.warn { "No kpi values to calculate." }
        }

        val hierarchyModel = getHierarchy()
        val kpiResult = KpiCalculator.calculateKpis(hierarchyModel, rawValueKpis)
        writeHierarchy(kpiResult)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeHierarchy(kpiResult: KpiResultHierarchy) {
        val outputFilePath = fileSystem.getPath(output)

        val directory = outputFilePath.toAbsolutePath().parent
        directory.createDirectories()

        outputFilePath.outputStream().use { Json.encodeToStream(kpiResult, it) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun getHierarchy(): KpiHierarchy {
        if (hierarchy == null) return DefaultHierarchy.get()

        fileSystem.getPath(hierarchy!!).inputStream().use {
            return Json.decodeFromStream<KpiHierarchy>(it)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun getKpiValuesFromSource(): List<RawValueKpi> {
        val location = fileSystem.getPath(this.sourceDir ?: "")

        val result = mutableListOf<RawValueKpi>()

        for (file in location.walk()) {
            if (file.extension.equals("json", true)) {
                readRawValueKpiFile(file, result)
            }
        }
        return result
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readRawValueKpiFile(file: Path, result: MutableList<RawValueKpi>) {
        file.inputStream().use {
            try {
                val kpis = Json.decodeFromStream<Collection<RawValueKpi>>(it)
                Logger.trace { "read kpi file '${file.absolutePathString()}'." }
                result.addAll(kpis)
            } catch (_: SerializationException) {
                // Log and ignore
                Logger.trace {
                    "could not deserialize '${file.absolutePathString()}' to a collection of raw kpi values."
                }
            }
        }
    }
}
