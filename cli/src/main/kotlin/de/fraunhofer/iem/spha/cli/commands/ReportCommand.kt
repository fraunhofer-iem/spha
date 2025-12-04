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
import com.github.ajalt.clikt.parameters.options.switch
import de.fraunhofer.iem.spha.cli.SphaToolCommandBase
import de.fraunhofer.iem.spha.cli.reporter.getMarkdown
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import java.nio.file.FileSystem
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class ReportCommand :
    SphaToolCommandBase(
        name = "report",
        help = "Takes a KpiResultHierarchy and generates a human readable report from it..",
    ),
    KoinComponent {

    private val fileSystem by inject<FileSystem>()

    private val resultHierarchy by
        option(
                "-r",
                "--resultHierarchy",
                help = "Path to the result hierarchy for which the report is generated.",
            )
            .required()

    private val exportFormat by option().switch("--markdown" to "markdown").required()

    private val output by
        option(
                "-o",
                "--output",
                help = "The output directory where the result of the operation is stored.",
            )
            .required()

    override suspend fun run() {
        super.run()

        val resultHierarchy = readResultHierarchy()
        val report =
            when (exportFormat) {
                "markdown" -> resultHierarchy.getMarkdown()
                else -> {
                    Logger.error { "Unknown exportFormat: $exportFormat" }
                    null
                }
            }

        report?.let { writeReport(it) }
    }

    private fun writeReport(report: String) {
        val outputFilePath = fileSystem.getPath(output)

        val directory = outputFilePath.toAbsolutePath().parent
        directory.createDirectories()

        Files.newBufferedWriter(outputFilePath).use { it.write(report) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readResultHierarchy(): KpiResultHierarchy {

        fileSystem.getPath(resultHierarchy).inputStream().use {
            return Json.decodeFromStream<KpiResultHierarchy>(it)
        }
    }
}
