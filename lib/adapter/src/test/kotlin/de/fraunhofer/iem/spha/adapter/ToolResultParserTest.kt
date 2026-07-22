/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
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

    // region Ticket 2: envelope-referenced-file exclusion + positive adapter matching

    private val secObserveExport =
        """{"results":[{"id":1,"vulnerability_id":"CVE-2020-0000","current_severity":"Critical","current_status":"Open","cve_found_in":[{"source":"CISA KEV"}]}]}"""

    private val cycloneDxSbom =
        """{"bomFormat":"CycloneDX","specVersion":"1.5","metadata":{"timestamp":"2020-01-01T00:00:00Z"},"components":[]}"""

    /** typeIds emitted by each gate adapter, used to assert which adapter claimed a file. */
    private fun typeIdsOf(result: AdapterResult<*>): Set<String> =
        result.transformationResults
            .filterIsInstance<TransformationResult.Success<*>>()
            .map { it.rawValueKpi.typeId }
            .toSet()

    @Test
    fun testEnvelopeReferencedDataFileIsNotProcessedTwice() {
        // A directory holding an envelope AND the data file it references (the `analyze` layout).
        // The data file must be processed exactly once — via its envelope — not also directly.
        val dir = createTempDirectory("envelope-plus-data")
        try {
            dir.resolve("secobserve.json").toFile().writeText(secObserveExport)
            dir.resolve("secobserve.envelope.json")
                .toFile()
                .writeText("""{"tool":"secobserve","result_file":"secobserve.json"}""")

            val results = ToolResultParser.parseJsonFilesFromDirectory(dir.toString())

            assertEquals(1, results.size, "data file should be processed once, via its envelope")
            assertEquals(
                setOf("KNOWN_EXPLOITED_VULNERABILITIES", "SEVERITY_THRESHOLD_FINDINGS"),
                typeIdsOf(results.single()),
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testCycloneDxSbomIsNotClaimedBySecObserveAdapter() {
        // A bare CycloneDX SBOM (no envelope). The permissive SecObserve DTO would previously
        // "match" it (results defaults to empty) and emit a degenerate, fail-open KPI. With
        // positive
        // discriminators it must be claimed by the CycloneDX adapter only.
        val dir = createTempDirectory("bare-cyclonedx")
        try {
            dir.resolve("sbom.cdx.json").toFile().writeText(cycloneDxSbom)

            val results = ToolResultParser.parseJsonFilesFromDirectory(dir.toString())

            assertEquals(1, results.size)
            assertEquals(setOf("SBOM_FRESHNESS"), typeIdsOf(results.single()))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testSecObserveExportIsNotClaimedByCycloneDxAdapter() {
        val dir = createTempDirectory("bare-secobserve")
        try {
            dir.resolve("secobserve.json").toFile().writeText(secObserveExport)

            val results = ToolResultParser.parseJsonFilesFromDirectory(dir.toString())

            assertEquals(1, results.size)
            assertEquals(
                setOf("KNOWN_EXPLOITED_VULNERABILITIES", "SEVERITY_THRESHOLD_FINDINGS"),
                typeIdsOf(results.single()),
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testMixedEnvelopeDirectoryProcessesEachLogicalInputExactlyOnce() {
        // The exact ticket scenario: two envelopes + their two data files in one directory.
        // Expect exactly two results (one SecObserve, one CycloneDX), no duplicates and no
        // cross-adapter mis-match.
        val dir = createTempDirectory("mixed-envelopes")
        try {
            dir.resolve("secobserve.json").toFile().writeText(secObserveExport)
            dir.resolve("secobserve.envelope.json")
                .toFile()
                .writeText("""{"tool":"secobserve","result_file":"secobserve.json"}""")
            dir.resolve("sbom.cdx.json").toFile().writeText(cycloneDxSbom)
            dir.resolve("sbom.envelope.json")
                .toFile()
                .writeText("""{"tool":"cyclonedx","result_file":"sbom.cdx.json"}""")

            val results = ToolResultParser.parseJsonFilesFromDirectory(dir.toString())

            assertEquals(2, results.size, "two logical inputs → two results, no duplicates")
            val emitted = results.map { typeIdsOf(it) }.toSet()
            assertEquals(
                setOf(
                    setOf("KNOWN_EXPLOITED_VULNERABILITIES", "SEVERITY_THRESHOLD_FINDINGS"),
                    setOf("SBOM_FRESHNESS"),
                ),
                emitted,
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    // endregion

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
