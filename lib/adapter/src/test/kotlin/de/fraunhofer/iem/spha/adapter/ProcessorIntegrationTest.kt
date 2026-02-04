/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import de.fraunhofer.iem.spha.model.adapter.OsvVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.TrivyVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.TrufflehogFindingDto
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests for processor-specific parsing through ToolResultParser.
 * Each test verifies that a specific tool's output file is correctly parsed
 * and produces the expected result types.
 */
class ProcessorIntegrationTest {

    private val testResourcesDir = "src/test/resources"

    @Test
    fun testParseOsvFile() {
        // Copy only the OSV file to a temporary directory
        val tempOsvDir = createTempDirectory("osv")
        val osvFile = File("$testResourcesDir/osv-scanner.json")
        val tempOsvFile = tempOsvDir.resolve("osv-scanner.json").toFile()
        osvFile.copyTo(tempOsvFile)

        val results = ToolResultParser.parseJsonFilesFromDirectory(tempOsvDir.toString())

        // Clean up
        tempOsvDir.toFile().deleteRecursively()

        assertTrue(results.isNotEmpty())

        results.forEach { adapterResult ->
            adapterResult.transformationResults.forEach {
                assertIs<TransformationResult.Success<OsvVulnerabilityDto>>(it)
            }
        }
    }

    @Test
    fun testParseTrivyFile() {
        // Copy only the Trivy file to a temporary directory
        val tempTrivyDir = createTempDirectory("trivy")
        val trivyFile = File("$testResourcesDir/trivy-result-v2.json")
        val tempTrivyFile = tempTrivyDir.resolve("trivy-result-v2.json").toFile()
        trivyFile.copyTo(tempTrivyFile)

        val results = ToolResultParser.parseJsonFilesFromDirectory(tempTrivyDir.toString())

        // Clean up
        tempTrivyDir.toFile().deleteRecursively()

        assertTrue(results.isNotEmpty())

        // Verify that all successful transformation results are of type TrivyVulnerabilityDto
        // Note: Some vulnerabilities may not have CVSS scores and will result in errors
        val allSuccesses = results.flatMap { adapterResult ->
            adapterResult.transformationResults.filterIsInstance<TransformationResult.Success<*>>()
        }
        assertTrue(allSuccesses.isNotEmpty(), "Expected at least one successful transformation result")
        allSuccesses.forEach {
            assertIs<TransformationResult.Success<TrivyVulnerabilityDto>>(it)
        }
    }

    @Test
    fun testParseTrufflehogFile() {
        // Copy only the Trufflehog file to a temporary directory
        val tempTrufflehogDir = createTempDirectory("trufflehog")
        val trufflehogFile = File("$testResourcesDir/trufflehog-ndjson.json")
        val tempTrufflehogFile = tempTrufflehogDir.resolve("trufflehog-ndjson.json").toFile()
        trufflehogFile.copyTo(tempTrufflehogFile)

        val results = ToolResultParser.parseJsonFilesFromDirectory(tempTrufflehogDir.toString())

        // Clean up
        tempTrufflehogDir.toFile().deleteRecursively()

        assertTrue(results.isNotEmpty())

        results.forEach { adapterResult ->
            adapterResult.transformationResults.forEach {
                assertIs<TransformationResult.Success<TrufflehogFindingDto>>(it)
            }
        }
    }

    @Test
    fun testParseTlcVueFile() {
        val jsonFiles = listOf(File("$testResourcesDir/techLag-npm-vuejs.json"))

        val results = ToolResultParser.getAdapterResultsFromJsonFiles(jsonFiles)

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun testParseTlcAngularFile() {
        val jsonFiles = listOf(File("$testResourcesDir/techLag-npm-angular.json"))

        val results = ToolResultParser.getAdapterResultsFromJsonFiles(jsonFiles)

        assertTrue(results.isNotEmpty())
    }
}
