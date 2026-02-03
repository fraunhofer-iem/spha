/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import de.fraunhofer.iem.spha.adapter.ToolProcessorImpl.Companion.jsonParser
import de.fraunhofer.iem.spha.adapter.tools.osv.OsvAdapter
import de.fraunhofer.iem.spha.adapter.tools.tlc.TlcAdapter
import de.fraunhofer.iem.spha.adapter.tools.trivy.TrivyAdapter
import de.fraunhofer.iem.spha.adapter.tools.trufflehog.TrufflehogAdapter
import de.fraunhofer.iem.spha.model.adapter.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

/** A processor that attempts to parse and transform content for a specific tool. */
private interface ToolProcessor {
    /** The name of the tool or format this processor handles, used for logging. */
    val name: String

    /** The unique identifier for this tool processor, used for configuration and identification. */
    val id: String

    /**
     * Tries to process the given JSON content.
     *
     * @param content The JSON string to process.
     * @return A list of `TransformationResult<Origin>` on success, or `null` if the content does
     *   not match this processor's format.
     * @throws Exception for unexpected errors during transformation logic.
     */
    fun tryProcess(content: String): AdapterResult<*>?
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
    override val id: String,
    private val serializer: KSerializer<T>,
    private val transform: (T) -> AdapterResult<*>,
) : ToolProcessor {

    override val name: String
        get() = serializer.descriptor.serialName

    override fun tryProcess(content: String): AdapterResult<*>? {
        return try {
            val resultObject = jsonParser.decodeFromString(serializer, content)
            transform(resultObject)
        } catch (_: SerializationException) {
            // This is an expected failure when the JSON does not match the DTO.
            // Return null to signal that the next processor should be tried.
            null
        }
    }

    companion object{
        val jsonParser = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }
    }
}

/**
 * A specialized processor for TruffleHog's NDJSON (newline-delimited JSON) output format. Each line
 * in the file is a separate JSON object representing a finding.
 */
private class TrufflehogNdjsonProcessor : ToolProcessor {
    private val serializer = TrufflehogFindingDto.serializer()

    override val name: String get() = serializer.descriptor.serialName
    override val id: String = ID

    override fun tryProcess(content: String): AdapterResult<*>? {
        val lines = content.lines().filter { it.isNotBlank() }

        // Must have at least one line and first line must look like a TruffleHog finding
        if (lines.isEmpty()) return null

        val findings = mutableListOf<TrufflehogFindingDto>()

        for (line in lines) {
            try {
                val finding = jsonParser.decodeFromString(serializer, line)
                // Verify this looks like a TruffleHog finding by checking for characteristic fields
                if (finding.detectorName != null || finding.sourceMetadata != null) {
                    findings.add(finding)
                } else {
                    // Line doesn't look like a TruffleHog finding, this isn't NDJSON format
                    return null
                }
            } catch (_: SerializationException) {
                // If any line fails to parse, this isn't valid NDJSON format
                return null
            }
        }

        if (findings.isEmpty()) return null

        // Convert findings to a TrufflehogReportDto by counting verified/unverified secrets
        val verifiedSecrets = findings.count { it.verified }
        val unverifiedSecrets = findings.count { !it.verified }

        val reportDto =
            TrufflehogReportDto(
                chunks = null,
                bytes = null,
                verifiedSecrets = verifiedSecrets,
                unverifiedSecrets = unverifiedSecrets,
                scanDuration = null,
                trufflehogVersion = null,
            )

        return TrufflehogAdapter.transformDataToKpi(reportDto)
    }

    companion object {
        const val ID = "trufflehog"
    }
}

object ToolResultParser {
    private val logger = KotlinLogging.logger {}

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
    fun parseJsonFilesFromDirectory(directoryPath: String): List<AdapterResult<*>> {

        val jsonFiles = getJsonFiles(directoryPath)

        if (jsonFiles.isEmpty()) {
            logger.info { "No .json files found in directory: $directoryPath" }
            return emptyList()
        }

        return getAdapterResultsFromJsonFiles(jsonFiles)
    }

    fun getAdapterResultsFromJsonFiles(jsonFiles: List<File>): List<AdapterResult<*>> {

        val transformationResults = mutableListOf<AdapterResult<*>>()
        for (file in jsonFiles) {
            try {
                val adapterResult = getAdapterResultFromJsonFile(file)
                if (adapterResult != null) {
                    transformationResults.add(adapterResult)
                }
            } catch (e: Exception) {
                logger.error { "Unexpected error processing file '${file.name}': ${e.message}" }
            }
        }
        return transformationResults
    }

    private fun getAdapterResultFromJsonFile(file: File): AdapterResult<*>? {
        val content = file.readText(Charsets.UTF_8)

        // TODO: Check for envelope format and use that first

        for (processor in ToolProcessorStore.processors.values) {
            try {
                // tryProcess returns results on success or null on format mismatch
                val results = processor.tryProcess(content)
                if (results != null) {
                    logger.info {
                        "Successfully parsed '${file.name}' as '${processor.name}'"
                    }
                    return results
                }
            } catch (e: Exception) {
                logger.error {
                    "Unexpected error processing '${file.name}' with '${processor.name}': ${e.message}"
                }
                break
            }
        }

        logger.warn { "No suitable tool processor found for '${file.name}' " }
        return null
    }
}

private object ToolProcessorStore {

    val processors = mapOf(
        "osv-scanner" to ToolProcessorImpl("osv-scanner", OsvScannerDto.serializer()) {
            OsvAdapter.transformDataToKpi(it)
        },
        "trivy" to ToolProcessorImpl("trivy", TrivyDtoV2.serializer()) {
            TrivyAdapter.transformDataToKpi(it)
        },
        TrufflehogNdjsonProcessor.ID to TrufflehogNdjsonProcessor(),
        "technicalLag" to ToolProcessorImpl("technicalLag", TlcDto.serializer()) {
            TlcAdapter.transformDataToKpi(it)
        },
    )
}
