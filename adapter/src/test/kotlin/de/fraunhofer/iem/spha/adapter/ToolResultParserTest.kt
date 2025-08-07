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
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

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
    fun testParseJsonFilesFromDirectoryWithOsvFile() {
        // Copy only the OSV file to a temporary directory
        val tempOsvDir = createTempDirectory("osv")
        val osvFile = File("$testResourcesDir/osv-scanner.json")
        val tempOsvFile = tempOsvDir.resolve("osv-scanner.json").toFile()
        osvFile.copyTo(tempOsvFile)

        val results = ToolResultParser.parseJsonFilesFromDirectory(tempOsvDir.toString())

        // Clean up
        tempOsvDir.toFile().deleteRecursively()

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it is AdapterResult.Success })
    }

    @Test
    fun testParseJsonFilesFromDirectoryWithTrivyFile() {
        // Copy only the Trivy file to a temporary directory
        val tempTrivyDir = createTempDirectory("trivy")
        val trivyFile = File("$testResourcesDir/trivy-result-v2.json")
        val tempTrivyFile = tempTrivyDir.resolve("trivy-result-v2.json").toFile()
        trivyFile.copyTo(tempTrivyFile)

        // We're just testing that the file is processed without errors
        // The actual results might be empty depending on the adapter implementation
        ToolResultParser.parseJsonFilesFromDirectory(tempTrivyDir.toString())

        // Clean up
        tempTrivyDir.toFile().deleteRecursively()
    }

    @Test
    fun testParseJsonFilesFromDirectoryWithTrufflehogFile() {
        // Copy only the Trufflehog file to a temporary directory
        val tempTrufflehogDir = createTempDirectory("trufflehog")
        val trufflehogFile = File("$testResourcesDir/trufflehog.json")
        val tempTrufflehogFile = tempTrufflehogDir.resolve("trufflehog.json").toFile()
        trufflehogFile.copyTo(tempTrufflehogFile)

        // We're just testing that the file is processed without errors
        // The actual results might be empty depending on the adapter implementation
        ToolResultParser.parseJsonFilesFromDirectory(tempTrufflehogDir.toString())

        // Clean up
        tempTrufflehogDir.toFile().deleteRecursively()
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
    fun testGetAdapterResultsFromJsonFiles() {
        val jsonFiles =
            listOf(
                File("$testResourcesDir/osv-scanner.json"),
                File("$testResourcesDir/trivy-result-v2.json"),
                File("$testResourcesDir/trufflehog.json"),
                File("$testResourcesDir/tlc-result-npm.json"),
            )

        val results = ToolResultParser.getAdapterResultsFromJsonFiles(jsonFiles)

        assertTrue(results.isNotEmpty())
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
}
