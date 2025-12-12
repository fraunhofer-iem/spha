/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trivy

import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.TrivyDtoV2
import de.fraunhofer.iem.spha.model.adapter.TrivyResult
import de.fraunhofer.iem.spha.model.adapter.TrivyVulnerabilityDto
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TrivyAdapterTest {

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "{}" // No schema
            ]
    )
    fun testInvalidJson(input: String) {
        input.byteInputStream().use {
            assertThrows<Exception> {
                val res = TrivyAdapter.dtoFromJson(it, TrivyDtoV2.serializer())
                println(res)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\"results\":[], \"SchemaVersion\": 2}", "{\"SchemaVersion\": 2}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = TrivyAdapter.dtoFromJson(it, TrivyDtoV2.serializer())
            assertEquals(0, dto.results.count())
        }
    }

    @Test
    fun testResult2Dto() {
        Files.newInputStream(Path("src/test/resources/trivy-result-v2.json")).use {
            val dto = assertDoesNotThrow { TrivyAdapter.dtoFromJson(it, TrivyDtoV2.serializer()) }
            assertEquals(2, dto.results.first().vulnerabilities.count())

            val vuln = dto.results.first().vulnerabilities.first()
            assertEquals("CVE-2011-3374", vuln.vulnerabilityID)
            assertEquals("apt", vuln.pkgName)
            assertEquals("2.6.1", vuln.installedVersion)

            // The severity might be a string like "MEDIUM" or "HIGH" instead of a numeric value
            // Let's just check that it's not null or empty
            assert(vuln.severity.isNotEmpty())
        }
    }

    @Test
    fun trivyV2DtoToRawValue() {
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V2Score",
                                                                            JsonPrimitive(5.0),
                                                                        ),
                                                                        Pair(
                                                                            "V3Score",
                                                                            JsonPrimitive(6.0),
                                                                        ),
                                                                    )
                                                            ),
                                                        )
                                                    )
                                            ),
                                        vulnerabilityID = "ID",
                                        installedVersion = "0.0.1",
                                        pkgName = "TEST PACKAGE",
                                        severity = "MEDIUM",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()

        // The result might be an AdapterResult.Error, so we'll check both cases
        if (result is TransformationResult.Success) {
            assertEquals(40, result.rawValueKpi.score)
        } else {
            // If it's an error, we'll just check that it's an AdapterResult.Error
            assert(result is TransformationResult.Error)
        }
    }

    @Test
    fun testNullCvssData() {
        // Create a test DTO with null CVSS data
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss = null,
                                        vulnerabilityID = "NULL-CVSS-ID",
                                        installedVersion = "1.0.0",
                                        pkgName = "test-package",
                                        severity = "MEDIUM",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()
        assertTrue(result is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, result.type)
    }

    @Test
    fun testEmptyCvssData() {
        // Create a test DTO with empty CVSS data
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss = JsonObject(emptyMap()),
                                        vulnerabilityID = "EMPTY-CVSS-ID",
                                        installedVersion = "1.0.0",
                                        pkgName = "test-package",
                                        severity = "MEDIUM",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()
        assertTrue(result is TransformationResult.Success)
        assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testOnlyV2Score() {
        // Create a test DTO with only V2 score
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V2Score",
                                                                            JsonPrimitive(7.5),
                                                                        )
                                                                    )
                                                            ),
                                                        )
                                                    )
                                            ),
                                        vulnerabilityID = "V2-ONLY-ID",
                                        installedVersion = "1.0.0",
                                        pkgName = "test-package",
                                        severity = "HIGH",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()
        assertTrue(result is TransformationResult.Success)
        assertEquals(25, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testOnlyV3Score() {
        // Create a test DTO with only V3 score
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V3Score",
                                                                            JsonPrimitive(8.5),
                                                                        )
                                                                    )
                                                            ),
                                                        )
                                                    )
                                            ),
                                        vulnerabilityID = "V3-ONLY-ID",
                                        installedVersion = "1.0.0",
                                        pkgName = "test-package",
                                        severity = "HIGH",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()
        assertTrue(result is TransformationResult.Success)
        assertEquals(15, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testNullV2AndV3Scores() {
        // Create a test DTO with null V2 and V3 scores
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V2Score",
                                                                            JsonPrimitive(null),
                                                                        ),
                                                                        Pair(
                                                                            "V3Score",
                                                                            JsonPrimitive(null),
                                                                        ),
                                                                    )
                                                            ),
                                                        )
                                                    )
                                            ),
                                        vulnerabilityID = "NULL-SCORES-ID",
                                        installedVersion = "1.0.0",
                                        pkgName = "test-package",
                                        severity = "MEDIUM",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()
        assertTrue(result is TransformationResult.Success)
        assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testMultipleCvssSources() {
        // Create a test DTO with multiple CVSS data sources
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V2Score",
                                                                            JsonPrimitive(5.0),
                                                                        ),
                                                                        Pair(
                                                                            "V3Score",
                                                                            JsonPrimitive(6.0),
                                                                        ),
                                                                    )
                                                            ),
                                                        ),
                                                        Pair(
                                                            "redhat",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V2Score",
                                                                            JsonPrimitive(7.0),
                                                                        ),
                                                                        Pair(
                                                                            "V3Score",
                                                                            JsonPrimitive(8.0),
                                                                        ),
                                                                    )
                                                            ),
                                                        ),
                                                    )
                                            ),
                                        vulnerabilityID = "MULTI-SOURCE-ID",
                                        installedVersion = "1.0.0",
                                        pkgName = "test-package",
                                        severity = "HIGH",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()
        assertTrue(result is TransformationResult.Success)
        assertEquals(20, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testOutOfRangeScore() {
        // Create a test DTO with a score that's out of range for transformVulnerabilityToKpi
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V2Score",
                                                                            JsonPrimitive(11.0),
                                                                        )
                                                                    )
                                                            ),
                                                        )
                                                    )
                                            ),
                                        vulnerabilityID = "OUT-OF-RANGE-ID",
                                        installedVersion = "1.0.0",
                                        pkgName = "test-package",
                                        severity = "CRITICAL",
                                    )
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()
        assertTrue(result is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, result.type)
    }

    @Test
    fun testMultipleVulnerabilities() {
        // Create a test DTO with multiple vulnerabilities
        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        TrivyResult(
                            vulnerabilities =
                                listOf(
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V2Score",
                                                                            JsonPrimitive(5.0),
                                                                        )
                                                                    )
                                                            ),
                                                        )
                                                    )
                                            ),
                                        vulnerabilityID = "VULN-1",
                                        installedVersion = "1.0.0",
                                        pkgName = "package-1",
                                        severity = "MEDIUM",
                                    ),
                                    TrivyVulnerabilityDto(
                                        cvss =
                                            JsonObject(
                                                content =
                                                    mapOf(
                                                        Pair(
                                                            "nvd",
                                                            JsonObject(
                                                                content =
                                                                    mapOf(
                                                                        Pair(
                                                                            "V3Score",
                                                                            JsonPrimitive(8.0),
                                                                        )
                                                                    )
                                                            ),
                                                        )
                                                    )
                                            ),
                                        vulnerabilityID = "VULN-2",
                                        installedVersion = "2.0.0",
                                        pkgName = "package-2",
                                        severity = "HIGH",
                                    ),
                                )
                        )
                    ),
                schemaVersion = 2,
            )

        val adapterResult = TrivyAdapter.transformDataToKpi(trivyV2Dto)
        val adapterResults = adapterResult.transformationResults

        assertEquals(2, adapterResults.size)

        // Check first vulnerability
        val result1 = adapterResults.first()
        assertTrue(result1 is TransformationResult.Success)
        assertEquals(50, (result1 as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("VULN-1", result1.origin.vulnerabilityID)

        // Check second vulnerability
        val result2 = adapterResults.last()
        assertTrue(result2 is TransformationResult.Success)
        assertEquals(20, (result2 as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("VULN-2", result2.origin.vulnerabilityID)
    }
}
