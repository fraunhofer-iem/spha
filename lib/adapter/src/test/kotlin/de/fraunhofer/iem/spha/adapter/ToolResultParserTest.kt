/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ToolResultParserTest {

    @TempDir lateinit var tempDir: Path

    private lateinit var testResourcesDir: String
    private lateinit var emptyDir: Path
    private lateinit var nonExistentDir: Path
    private lateinit var invalidJsonDir: Path

    @BeforeEach
    fun setUp() {
        testResourcesDir = "src/test/resources"
        emptyDir = createTempDirectory("empty")
        nonExistentDir = Path.of("non-existent-directory")
        invalidJsonDir = createTempDirectory("invalid")

        // Create an invalid JSON file
        val invalidJsonFile = invalidJsonDir.resolve("invalid.json").toFile()
        invalidJsonFile.writeText("{invalid json")
    }

    @AfterEach
    fun tearDown() {
        emptyDir.toFile().deleteRecursively()
        invalidJsonDir.toFile().deleteRecursively()
    }

    @Test
    fun testGetJsonFilesWithValidDirectory() {
        val jsonFiles = ToolResultParser.parseJsonFilesFromDirectory(testResourcesDir)
        assertTrue(jsonFiles.isNotEmpty())
    }

    @Test
    fun testGetJsonFilesWithEmptyDirectory() {
        val jsonFiles = ToolResultParser.parseJsonFilesFromDirectory(emptyDir.toString())
        assertTrue(jsonFiles.isEmpty())
    }

    @Test
    fun testGetJsonFilesWithNonExistentDirectory() {
        val jsonFiles = ToolResultParser.parseJsonFilesFromDirectory(nonExistentDir.toString())
        assertTrue(jsonFiles.isEmpty())
    }

    @Test
    fun testParseJsonFilesFromDirectoryWithInvalidJsonFile() {
        val results = ToolResultParser.parseJsonFilesFromDirectory(invalidJsonDir.toString())
        assertTrue(results.isEmpty())
    }

    @Test
    fun testParseJsonFilesFromDirectoryWithEmptyJsonFile() {
        // Create an empty JSON file
        val emptyJsonDir = createTempDirectory("empty-json")
        val emptyJsonFile = emptyJsonDir.resolve("empty.json").toFile()
        emptyJsonFile.writeText("")

        val results = ToolResultParser.parseJsonFilesFromDirectory(emptyJsonDir.toString())

        // Clean up
        emptyJsonDir.toFile().deleteRecursively()

        assertTrue(results.isEmpty())
    }

    @Test
    fun testParseJsonFilesFromDirectoryIgnoresNonJsonFiles() {
        // Create a directory with both JSON and non-JSON files
        val mixedDir = createTempDirectory("mixed-files")

        // Copy a valid JSON file
        val osvFile = File("$testResourcesDir/osv-scanner.json")
        val jsonFile = mixedDir.resolve("osv-scanner.json").toFile()
        osvFile.copyTo(jsonFile)

        // Create non-JSON files that should be ignored
        val txtFile = mixedDir.resolve("readme.txt").toFile()
        txtFile.writeText("This is a text file")

        val xmlFile = mixedDir.resolve("config.xml").toFile()
        xmlFile.writeText("<config><setting>value</setting></config>")

        val csvFile = mixedDir.resolve("data.csv").toFile()
        csvFile.writeText("col1,col2\nval1,val2")

        val noExtensionFile = mixedDir.resolve("Dockerfile").toFile()
        noExtensionFile.writeText("FROM alpine:latest")

        val results = ToolResultParser.parseJsonFilesFromDirectory(mixedDir.toString())

        // Clean up
        mixedDir.toFile().deleteRecursively()

        // Should only parse the JSON file, ignoring all non-JSON files
        assertEquals(1, results.size)
        // Verify that the result contains successful transformations
        val allSuccesses =
            results.flatMap { adapterResult ->
                adapterResult.transformationResults.filterIsInstance<
                    TransformationResult.Success<*>
                >()
            }
        assertTrue(
            allSuccesses.isNotEmpty(),
            "Expected at least one successful transformation result",
        )
    }

    @Test
    fun testGetAdapterResultsFromJsonFiles() {
        val (envelopeDir, envelopeFile) = createEnvelopeFile("trufflehog", "trufflehog-ndjson.json")

        try {
            val jsonFiles =
                listOf(
                    File("$testResourcesDir/osv-scanner.json"),
                    File("$testResourcesDir/trivy-result-v2.json"),
                    envelopeFile,
                    File("$testResourcesDir/techLag-npm-vuejs.json"),
                )

            val results = ToolResultParser.getAdapterResultsFromJsonFiles(jsonFiles)

            assertEquals(4, results.size)
        } finally {
            envelopeDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testGetAdapterResultsFromJsonFilesWithInvalidFile() {
        val jsonFiles = listOf(invalidJsonDir.resolve("invalid.json").toFile())

        val results = ToolResultParser.getAdapterResultsFromJsonFiles(jsonFiles)

        assertTrue(results.isEmpty())
    }

    @Test
    fun testGetAdapterResultsFromJsonFilesWithEmptyFile() {
        // Create an empty JSON file
        val emptyJsonFile = tempDir.resolve("empty.json").toFile()
        emptyJsonFile.writeText("")

        val jsonFiles = listOf(emptyJsonFile)

        val results = ToolResultParser.getAdapterResultsFromJsonFiles(jsonFiles)

        assertTrue(results.isEmpty())
    }

    @Test
    fun testGetAdapterResultsFromJsonFilesWithMixedFiles() {
        // Create an invalid JSON file
        val invalidJsonFile = tempDir.resolve("invalid.json").toFile()
        invalidJsonFile.writeText("{invalid json")

        val jsonFiles =
            listOf(
                File("$testResourcesDir/osv-scanner.json"),
                invalidJsonFile,
                File("$testResourcesDir/trivy-result-v2.json"),
            )

        val results = ToolResultParser.getAdapterResultsFromJsonFiles(jsonFiles)

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun testEnvelopeFormatWithRelativePath() {
        // Test envelope format with relative path to result file
        // Create envelope and result file in same directory to test relative path resolution
        val (envelopeDir, envelopeFile) =
            createEnvelopeFileWithPath("trufflehog", "trufflehog-ndjson.json")
        // Copy result file to envelope directory for relative path resolution
        val resultFile = File("$testResourcesDir/trufflehog-ndjson.json")
        resultFile.copyTo(envelopeDir.resolve("trufflehog-ndjson.json").toFile())

        try {
            val results = ToolResultParser.getAdapterResultsFromJsonFiles(listOf(envelopeFile))

            assertTrue(results.isNotEmpty())
            results.forEach {
                assertTrue(it.transformationResults.all { it is TransformationResult.Success<*> })
            }
        } finally {
            envelopeDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testEnvelopeFormatWithAbsolutePath() {
        val (envelopeDir, envelopeFile) = createEnvelopeFile("trufflehog", "trufflehog-ndjson.json")

        try {
            val results = ToolResultParser.getAdapterResultsFromJsonFiles(listOf(envelopeFile))

            assertTrue(results.isNotEmpty())
            results.forEach { it ->
                assertTrue(it.transformationResults.all { it is TransformationResult.Success<*> })
            }
        } finally {
            envelopeDir.toFile().deleteRecursively()
        }
    }

    @ParameterizedTest
    @MethodSource("invalidEnvelopeTestCases")
    fun testInvalidEnvelopeReturnsEmptyResult(
        toolId: String,
        resultFilePath: String,
        description: String,
    ) {
        val (envelopeDir, envelopeFile) = createEnvelopeFileWithPath(toolId, resultFilePath)

        try {
            val results = ToolResultParser.getAdapterResultsFromJsonFiles(listOf(envelopeFile))
            assertTrue(results.isEmpty(), "Expected empty results for: $description")
        } finally {
            envelopeDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testEnvelopeFormatWithEmptyTrufflehogResultFile() {
        val (envelopeDir, envelopeFile) =
            createEnvelopeFileWithPath("trufflehog", "empty-result.json")
        envelopeDir.resolve("empty-result.json").toFile().writeText("")

        try {
            val results = ToolResultParser.getAdapterResultsFromJsonFiles(listOf(envelopeFile))
            assertTrue(results.isNotEmpty())
            val adapterResult = results.first()
            val kpis = adapterResult.transformationResults
            assertTrue(kpis.all { it is TransformationResult.Success<*> })
            assertEquals(100, (kpis.first() as TransformationResult.Success.Kpi).rawValueKpi.score)
        } finally {
            envelopeDir.toFile().deleteRecursively()
        }
    }

    @ParameterizedTest
    @MethodSource("envelopeTestCases")
    fun testEnvelopeFilesProduceSuccessfulResults(toolId: String, resultFileName: String) {
        val (envelopeDir, envelopeFile) = createEnvelopeFile(toolId, resultFileName)

        try {
            val results = ToolResultParser.getAdapterResultsFromJsonFiles(listOf(envelopeFile))

            assertTrue(
                results.isNotEmpty(),
                "Expected non-empty results for tool: $toolId, file: $resultFileName",
            )

            // Verify that at least one transformation result is a success
            val allSuccesses =
                results.flatMap { adapterResult ->
                    adapterResult.transformationResults.filterIsInstance<
                        TransformationResult.Success<*>
                    >()
                }
            assertTrue(
                allSuccesses.isNotEmpty(),
                "Expected at least one successful transformation result for tool: $toolId",
            )
        } finally {
            envelopeDir.toFile().deleteRecursively()
        }
    }

    private fun createEnvelopeFile(toolId: String, resultFileName: String): Pair<Path, File> {
        val resultFile = File("$testResourcesDir/$resultFileName")
        return createEnvelopeFileWithPath(toolId, resultFile.absolutePath.replace("\\", "/"))
    }

    private fun createEnvelopeFileWithPath(
        toolId: String,
        resultFilePath: String,
    ): Pair<Path, File> {
        val envelopeDir = createTempDirectory("envelope-$toolId")
        val envelopeFile = envelopeDir.resolve("envelope.json").toFile()
        envelopeFile.writeText(
            """
            {
              "tool": "$toolId",
              "result_file": "$resultFilePath"
            }
        """
                .trimIndent()
        )
        return Pair(envelopeDir, envelopeFile)
    }

    companion object {
        private val testResourcesDir = "src/test/resources"

        @JvmStatic
        fun envelopeTestCases() =
            listOf(
                Arguments.of("osv-scanner", "osv-scanner.json"),
                Arguments.of("trivy", "trivy-result-v2.json"),
                Arguments.of("trufflehog", "trufflehog-ndjson.json"),
                Arguments.of("technicalLag", "techLag-npm-vuejs.json"),
            )

        @JvmStatic
        fun invalidEnvelopeTestCases(): List<Arguments> {
            val validResultFile = File("$testResourcesDir/trufflehog-ndjson.json")
            val osvResultFile = File("$testResourcesDir/osv-scanner.json")
            val testResourcesDirAbsolute = File(testResourcesDir).absolutePath.replace("\\", "/")
            return listOf(
                Arguments.of(
                    "unknown-tool",
                    validResultFile.absolutePath.replace("\\", "/"),
                    "unknown tool ID",
                ),
                Arguments.of(
                    "trivy",
                    osvResultFile.absolutePath.replace("\\", "/"),
                    "trivy tool ID with osv-scanner file",
                ),
                Arguments.of("trufflehog", "non-existent-file.json", "missing file reference"),
                Arguments.of(
                    "trufflehog",
                    testResourcesDirAbsolute,
                    "reference to directory instead of file",
                ),
            )
        }
    }
}
