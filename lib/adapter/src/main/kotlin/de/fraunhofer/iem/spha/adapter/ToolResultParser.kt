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
import de.fraunhofer.iem.spha.model.adapter.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Paths
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** A processor that attempts to parse and transform content for a specific tool. */
internal interface ToolProcessor {
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

    companion object {
        val jsonParser = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }
    }
}

/**
 * A generic implementation of [ToolProcessor] that links a serializer to its transformation logic.
 *
 * @param T The specific [ToolResult] DTO type.
 * @property serializer The `KSerializer` for the type `T`.
 * @property transform The function to convert the decoded object of type `T` into the result.
 */
internal class ToolProcessorImpl<T : ToolResult>(
    override val id: String,
    private val serializer: KSerializer<T>,
    private val transform: (T) -> AdapterResult<*>,
) : ToolProcessor {

    override val name: String
        get() = serializer.descriptor.serialName

    override fun tryProcess(content: String): AdapterResult<*>? {
        return try {
            val resultObject = ToolProcessor.jsonParser.decodeFromString(serializer, content)
            transform(resultObject)
        } catch (_: SerializationException) {
            // This is an expected failure when the JSON does not match the DTO.
            // Return null to signal that the next processor should be tried.
            null
        }
    }
}

/**
 * A specialized processor for TruffleHog's NDJSON (newline-delimited JSON) output format. Each line
 * in the file is a separate JSON object representing a finding.
 */
internal class TrufflehogNdjsonProcessor : ToolProcessor {
    private val serializer = TrufflehogFindingDto.serializer()

    override val name: String
        get() = serializer.descriptor.serialName

    override val id: String = ID

    override fun tryProcess(content: String): AdapterResult<*>? {
        val lines = content.lines().filter { it.isNotBlank() }

        val findings = mutableListOf<TrufflehogFindingDto>()

        for (line in lines) {
            try {
                val finding = ToolProcessor.jsonParser.decodeFromString(serializer, line)
                findings.add(finding)
            } catch (_: SerializationException) {
                return null
            }
        }

        val reportDto =
            TrufflehogResultDto(
                verifiedSecrets = findings.count { it.verified },
                unverifiedSecrets = findings.count { !it.verified },
                origins = findings,
            )

        return TrufflehogAdapter.transformDataToKpi(reportDto)
    }

    companion object {
        const val ID = "trufflehog"
    }
}

object ToolResultParser {
    private val logger = KotlinLogging.logger {}

    private val envelopeJsonParser = Json

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

        when (val envelopeResult = tryProcessEnvelope(content, file.parentFile)) {
            is EnvelopeProcessResult.Success -> {
                logger.info { "Successfully parsed '${file.name}' as envelope format" }
                return envelopeResult.result
            }
            is EnvelopeProcessResult.Failed -> {
                logger.error {
                    "Envelope '${file.name}' failed to process: ${envelopeResult.reason}"
                }
                // Envelope was detected, but processing failed - do NOT fall back to other
                // processors
                return null
            }
            is EnvelopeProcessResult.NotAnEnvelope -> {
                // Not an envelope, continue with other processors
            }
        }

        // At this point empty files are not allowed because they would cause an ambiguity.
        // It would be undecidable which tool produced the file, and thus
        // the first processor that supports empty files would always win.
        if (content.isBlank()) {
            return null
        }

        for (processor in ToolProcessorStore.processors.values) {
            try {
                // tryProcess returns results on success or null on format mismatch
                val results = processor.tryProcess(content)
                if (results != null) {
                    logger.info { "Successfully parsed '${file.name}' as '${processor.name}'" }
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

    /**
     * Tries to parse the content as an envelope format and process the referenced result file.
     *
     * @param content The JSON content to parse as an envelope.
     * @param baseDir The base directory to resolve relative paths in the envelope.
     * @return EnvelopeProcessResult indicating whether this was an envelope and if processing
     *   succeeded.
     */
    private fun tryProcessEnvelope(content: String, baseDir: File?): EnvelopeProcessResult {
        val envelope =
            try {
                envelopeJsonParser.decodeFromString(ToolResultEnvelope.serializer(), content)
            } catch (_: SerializationException) {
                return EnvelopeProcessResult.NotAnEnvelope
            }

        // Find the processor for the specified tool_id
        val processor =
            ToolProcessorStore.processors[envelope.tool]
                ?: return EnvelopeProcessResult.Failed(
                    "No processor found for tool_id '${envelope.tool}'"
                )

        // Resolve the result file path (can be absolute or relative to the envelope file)
        val resultFilePath = Paths.get(envelope.resultFile)
        val resultFile =
            if (resultFilePath.isAbsolute) {
                resultFilePath.toFile()
            } else {
                baseDir?.resolve(envelope.resultFile) ?: File(envelope.resultFile)
            }

        if (!resultFile.exists() || !resultFile.isFile) {
            return EnvelopeProcessResult.Failed(
                "Result file '${resultFile.absolutePath}' does not exist"
            )
        }

        // Read and process the result file content
        val resultContent = resultFile.readText(Charsets.UTF_8)
        return try {
            val result = processor.tryProcess(resultContent)
            if (result != null) {
                logger.info {
                    "Successfully processed result file '${resultFile.name}' with processor '${processor.name}'"
                }
                EnvelopeProcessResult.Success(result)
            } else {
                EnvelopeProcessResult.Failed(
                    "Processor '${processor.name}' could not parse result file '${resultFile.name}'"
                )
            }
        } catch (e: Exception) {
            EnvelopeProcessResult.Failed(
                "Error processing result file '${resultFile.name}': ${e.message}"
            )
        }
    }
}

/** Result of attempting to process an envelope format. */
private sealed class EnvelopeProcessResult {
    /** The content was not an envelope format - try other processors. */
    data object NotAnEnvelope : EnvelopeProcessResult()

    /** The content was an envelope format and processing succeeded. */
    data class Success(val result: AdapterResult<*>) : EnvelopeProcessResult()

    /** The content was an envelope format but processing failed - do NOT try other processors. */
    data class Failed(val reason: String) : EnvelopeProcessResult()
}

internal object ToolProcessorStore {

    val processors =
        mapOf(
            "osv-scanner" to
                ToolProcessorImpl("osv-scanner", OsvScannerDto.serializer()) {
                    OsvAdapter.transformDataToKpi(it)
                },
            "trivy" to
                ToolProcessorImpl("trivy", TrivyDtoV2.serializer()) {
                    TrivyAdapter.transformDataToKpi(it)
                },
            TrufflehogNdjsonProcessor.ID to TrufflehogNdjsonProcessor(),
            "technicalLag" to
                ToolProcessorImpl("technicalLag", TlcDto.serializer()) {
                    TlcAdapter.transformDataToKpi(it)
                },
        )
}
