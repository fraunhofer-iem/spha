/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import de.fraunhofer.iem.spha.adapter.tools.osv.OsvAdapter
import de.fraunhofer.iem.spha.adapter.tools.tlc.TlcAdapter
import de.fraunhofer.iem.spha.adapter.tools.trivy.TrivyAdapter
import de.fraunhofer.iem.spha.adapter.tools.trufflehog.TrufflehogAdapter
import de.fraunhofer.iem.spha.model.adapter.Origin
import de.fraunhofer.iem.spha.model.adapter.OsvScannerDto
import de.fraunhofer.iem.spha.model.adapter.TlcDto
import de.fraunhofer.iem.spha.model.adapter.ToolResult
import de.fraunhofer.iem.spha.model.adapter.TrivyDtoV2
import de.fraunhofer.iem.spha.model.adapter.TrufflehogDto
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** A processor that attempts to parse and transform content for a specific tool. */
private interface ToolProcessor {
    /** The name of the tool or format this processor handles, used for logging. */
    val name: String

    /**
     * Tries to process the given JSON content.
     *
     * @param content The JSON string to process.
     * @return A list of `AdapterResult<Origin>` on success, or `null` if the content does not match
     *   this processor's format.
     * @throws Exception for unexpected errors during transformation logic.
     */
    fun tryProcess(content: String): Collection<AdapterResult<Origin>>?
}

/**
 * A generic implementation of [ToolProcessor] that links a serializer to its transformation logic.
 *
 * @param T The specific [ToolResult] DTO type.
 * @property serializer The `KSerializer` for the type `T`.
 * @property jsonParser The `Json` instance to use for decoding.
 * @property transform The function to convert the decoded object of type `T` into the result.
 */
private class ToolProcessorImpl<T : ToolResult>(
    private val serializer: KSerializer<T>,
    private val jsonParser: Json,
    private val transform: (T) -> Collection<AdapterResult<Origin>>,
) : ToolProcessor {

    override val name: String
        get() = serializer.descriptor.serialName

    override fun tryProcess(content: String): Collection<AdapterResult<Origin>>? {
        return try {
            val resultObject = jsonParser.decodeFromString(serializer, content)
            transform(resultObject)
        } catch (_: SerializationException) {
            // This is an expected failure when the JSON does not match the DTO.
            // Return null to signal that the next processor should be tried.
            null
        }
    }
}

object ToolResultParser {

    private val logger = KotlinLogging.logger {}

    private val jsonParser = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    // List of all supported tool processors. Adding a new tool is a single-line change here.
    private val processors: List<ToolProcessor> =
        listOf(
            ToolProcessorImpl(OsvScannerDto.serializer(), jsonParser) {
                OsvAdapter.transformDataToKpi(it)
            },
            ToolProcessorImpl(TrivyDtoV2.serializer(), jsonParser) {
                TrivyAdapter.transformDataToKpi(it)
            },
            ToolProcessorImpl(TrufflehogDto.serializer(), jsonParser) {
                TrufflehogAdapter.transformDataToKpi(it)
            },
            ToolProcessorImpl(TlcDto.serializer(), jsonParser) { TlcAdapter.transformDataToKpi(it) },
        )

    private fun getJsonFiles(directoryPath: String): List<File> {
        val directory = File(directoryPath)

        if (!directory.exists() || !directory.isDirectory) {
            logger.error { "Error: Directory not found at path: $directoryPath" }
            return emptyList()
        }

        return directory
            .listFiles { _, name -> name.endsWith(".json", ignoreCase = true) }
            ?.toList() ?: emptyList()
    }

    /**
     * Parses all `.json` files from the specified directory, processes their content, and returns a
     * list of results. Each result is wrapped in an `AdapterResult`, which can either be a
     * `Success` or an `Error`. To parse the json files, all serializers implementing the KpiAdapter
     * class are tested.
     *
     * @param directoryPath the path to the directory from which `.json` files will be retrieved and
     *   processed
     * @return a list of `AdapterResult<Origin>` containing the results of parsing and processing
     *   the `.json` files
     */
    fun parseJsonFilesFromDirectory(directoryPath: String): List<AdapterResult<Origin>> {

        val jsonFiles = getJsonFiles(directoryPath)

        if (jsonFiles.isEmpty()) {
            logger.info { "No .json files found in directory: $directoryPath" }
            return emptyList()
        }

        return getAdapterResultsFromJsonFiles(jsonFiles)
    }

    fun getAdapterResultsFromJsonFiles(jsonFiles: List<File>): List<AdapterResult<Origin>> {

        val adapterResults = mutableListOf<AdapterResult<Origin>>()
        for (file in jsonFiles) {
            try {
                val content = file.readText(Charsets.UTF_8)
                if (content.isBlank()) {
                    logger.warn { "Warning: Skipping empty file: ${file.name}" }
                    continue
                }

                for (processor in processors) {
                    try {
                        // tryProcess returns results on success or null on format mismatch
                        val results = processor.tryProcess(content)
                        if (results != null) {
                            adapterResults.addAll(results)
                            logger.info {
                                "Successfully parsed '${file.name}' as '${processor.name}'"
                            }
                            break // Move to the next file
                        }
                    } catch (e: Exception) {
                        // Catch unexpected errors from the transform logic
                        logger.error {
                            "Unexpected error processing '${file.name}' with '${processor.name}': ${e.message}"
                        }
                        // Mark as parsed to avoid the "unsuitable parser" warning
                        break
                    }
                }
            } catch (e: Exception) {
                logger.error { "Error reading file '${file.name}': ${e.message}" }
            }
        }
        return adapterResults
    }
}
