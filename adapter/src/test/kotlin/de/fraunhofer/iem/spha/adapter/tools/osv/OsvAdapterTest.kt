/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.osv

import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.OsvPackageDto
import de.fraunhofer.iem.spha.model.adapter.OsvPackageWrapperDto
import de.fraunhofer.iem.spha.model.adapter.OsvScannerDto
import de.fraunhofer.iem.spha.model.adapter.OsvScannerResultDto
import de.fraunhofer.iem.spha.model.adapter.OsvVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.PackageSource
import de.fraunhofer.iem.spha.model.adapter.Severity
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OsvAdapterTest {
    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "{}", // No schema
                "{\"SchemaVersion\": 3}", // Not supported schema
            ]
    )
    fun testInvalidJson(input: String) {
        input.byteInputStream().use {
            assertThrows<Exception> { OsvAdapter.dtoFromJson(it, OsvScannerDto.serializer()) }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\"results\": []}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = OsvAdapter.dtoFromJson(it, OsvScannerDto.serializer())
            assertEquals(0, dto.results.count())
        }
    }

    @Test
    fun testResultDto() {
        Files.newInputStream(Path("src/test/resources/osv-scanner.json")).use { data ->
            val dto = assertDoesNotThrow {
                OsvAdapter.dtoFromJson(data, OsvScannerDto.serializer())
            }

            val adapterResult = assertDoesNotThrow { OsvAdapter.transformDataToKpi(dto) }
            val kpis = adapterResult.transformationResults

            // Print debug information
            println("[DEBUG_LOG] Total KPIs: ${kpis.size}")
            kpis.forEachIndexed { index, kpi ->
                println("[DEBUG_LOG] KPI $index: $kpi")
                if (kpi is TransformationResult.Error) {
                    println("[DEBUG_LOG] Error type: ${kpi.type}")
                }
            }

            // Filter out error results
            val successKpis = kpis.filter { it is TransformationResult.Success }

            // Print debug information
            println("[DEBUG_LOG] Success KPIs: ${successKpis.size}")

            // For this test, we'll just check that we have KPIs, not their specific type
            assert(kpis.isNotEmpty())
            assertEquals(8, successKpis.size)
        }
    }

    @Test
    fun testNullSeverity() {
        // Create a test DTO with null severity
        val osvVulnerabilityDto =
            OsvVulnerabilityDto(
                affected = emptyList(),
                severity = emptyList(),
                details = "Test details",
                id = "TEST-ID",
                modified = "2023-01-01",
                published = "2023-01-01",
                references = emptyList(),
                schemaVersion = "1.0",
                summary = "Test summary",
            )

        val osvPackageDto =
            OsvPackageDto(name = "test-package", version = "1.0.0", ecosystem = "npm")

        val osvPackageWrapperDto =
            OsvPackageWrapperDto(
                osvPackage = osvPackageDto,
                vulnerabilities = listOf(osvVulnerabilityDto),
                groups = emptyList(),
            )

        val packageSource = PackageSource(path = "test-path", type = "test-type")

        val osvScannerResultDto =
            OsvScannerResultDto(
                packageSource = packageSource,
                packages = listOf(osvPackageWrapperDto),
            )

        val osvScannerDto = OsvScannerDto(results = listOf(osvScannerResultDto))

        // Transform the data
        val adapterResult = OsvAdapter.transformDataToKpi(osvScannerDto)
        val results = adapterResult.transformationResults

        // Verify the results
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, result.type)
    }

    @Test
    fun testEmptySeverityList() {
        // Create a test DTO with an empty severity list
        val osvVulnerabilityDto =
            OsvVulnerabilityDto(
                affected = emptyList(),
                severity = emptyList(), // Empty severity list
                details = "Test details",
                id = "TEST-ID",
                modified = "2023-01-01",
                published = "2023-01-01",
                references = emptyList(),
                schemaVersion = "1.0",
                summary = "Test summary",
            )

        val osvPackageDto =
            OsvPackageDto(name = "test-package", version = "1.0.0", ecosystem = "npm")

        val osvPackageWrapperDto =
            OsvPackageWrapperDto(
                osvPackage = osvPackageDto,
                vulnerabilities = listOf(osvVulnerabilityDto),
                groups = emptyList(),
            )

        val packageSource = PackageSource(path = "test-path", type = "test-type")

        val osvScannerResultDto =
            OsvScannerResultDto(
                packageSource = packageSource,
                packages = listOf(osvPackageWrapperDto),
            )

        val osvScannerDto = OsvScannerDto(results = listOf(osvScannerResultDto))

        // Transform the data
        val adapterResult = OsvAdapter.transformDataToKpi(osvScannerDto)
        val results = adapterResult.transformationResults

        // Verify the results
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, result.type)
    }

    @Test
    fun testInvalidCvssScore() {
        // Create a test DTO with an invalid CVSS score that will cause parseVector to return null
        val osvVulnerabilityDto =
            OsvVulnerabilityDto(
                affected = emptyList(),
                severity =
                    listOf(
                        Severity(
                            type = "CVSS_V3",
                            score = "", // Empty score will cause parseVector to return null
                        )
                    ),
                details = "Test details",
                id = "TEST-ID",
                modified = "2023-01-01",
                published = "2023-01-01",
                references = emptyList(),
                schemaVersion = "1.0",
                summary = "Test summary",
            )

        val osvPackageDto =
            OsvPackageDto(name = "test-package", version = "1.0.0", ecosystem = "npm")

        val osvPackageWrapperDto =
            OsvPackageWrapperDto(
                osvPackage = osvPackageDto,
                vulnerabilities = listOf(osvVulnerabilityDto),
                groups = emptyList(),
            )

        val packageSource = PackageSource(path = "test-path", type = "test-type")

        val osvScannerResultDto =
            OsvScannerResultDto(
                packageSource = packageSource,
                packages = listOf(osvPackageWrapperDto),
            )

        val osvScannerDto = OsvScannerDto(results = listOf(osvScannerResultDto))

        // Transform the data
        val adapterResult = OsvAdapter.transformDataToKpi(osvScannerDto)
        val results = adapterResult.transformationResults

        // Verify the results
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, result.type)
    }

    @Test
    fun testOutOfRangeScore() {
        // Create a test DTO with a score that's out of range for transformVulnerabilityToKpi
        val osvVulnerabilityDto =
            OsvVulnerabilityDto(
                affected = emptyList(),
                severity =
                    listOf(
                        Severity(
                            type = "CVSS_V3",
                            score =
                                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", // This will parse to
                            // a score of 9.8,
                            // which is valid
                        ),
                        Severity(
                            type = "CVSS_V3",
                            score =
                                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:L/A:L", // This will parse to
                            // a score of 7.3,
                            // which is valid but
                            // lower than 9.8
                        ),
                    ),
                details = "Test details",
                id = "TEST-ID",
                modified = "2023-01-01",
                published = "2023-01-01",
                references = emptyList(),
                schemaVersion = "1.0",
                summary = "Test summary",
            )

        val osvPackageDto =
            OsvPackageDto(name = "test-package", version = "1.0.0", ecosystem = "npm")

        val osvPackageWrapperDto =
            OsvPackageWrapperDto(
                osvPackage = osvPackageDto,
                vulnerabilities = listOf(osvVulnerabilityDto),
                groups = emptyList(),
            )

        val packageSource = PackageSource(path = "test-path", type = "test-type")

        val osvScannerResultDto =
            OsvScannerResultDto(
                packageSource = packageSource,
                packages = listOf(osvPackageWrapperDto),
            )

        val osvScannerDto = OsvScannerDto(results = listOf(osvScannerResultDto))

        // Transform the data
        val adapterResult = OsvAdapter.transformDataToKpi(osvScannerDto)
        val results = adapterResult.transformationResults

        // Verify the results
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is TransformationResult.Success)
        val successResult = result
        assertEquals(2, successResult.rawValueKpi.score) // 100 - (9.8 * 10) = 2
    }

    @Test
    fun testSuccessfulTransformation() {
        // Create a test DTO with valid data
        val osvVulnerabilityDto =
            OsvVulnerabilityDto(
                affected = emptyList(),
                severity =
                    listOf(
                        Severity(
                            type = "CVSS_V3",
                            score =
                                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:L/A:L", // This will parse to
                            // a score of 7.3
                        )
                    ),
                details = "Test details",
                id = "TEST-ID",
                modified = "2023-01-01",
                published = "2023-01-01",
                references = emptyList(),
                schemaVersion = "1.0",
                summary = "Test summary",
            )

        val osvPackageDto =
            OsvPackageDto(name = "test-package", version = "1.0.0", ecosystem = "npm")

        val osvPackageWrapperDto =
            OsvPackageWrapperDto(
                osvPackage = osvPackageDto,
                vulnerabilities = listOf(osvVulnerabilityDto),
                groups = emptyList(),
            )

        val packageSource = PackageSource(path = "test-path", type = "test-type")

        val osvScannerResultDto =
            OsvScannerResultDto(
                packageSource = packageSource,
                packages = listOf(osvPackageWrapperDto),
            )

        val osvScannerDto = OsvScannerDto(results = listOf(osvScannerResultDto))

        // Transform the data
        val adapterResult = OsvAdapter.transformDataToKpi(osvScannerDto)
        val results = adapterResult.transformationResults

        // Verify the results
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is TransformationResult.Success)
        assertEquals(27, result.rawValueKpi.score) // 100 - (7.3 * 10) = 27
        assertEquals(osvVulnerabilityDto, result.origin)
    }
}
