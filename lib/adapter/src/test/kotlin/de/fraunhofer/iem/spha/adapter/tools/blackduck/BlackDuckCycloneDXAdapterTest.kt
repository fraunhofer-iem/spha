/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.blackduck

import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.CycloneDXDto
import de.fraunhofer.iem.spha.model.adapter.CycloneDXRating
import de.fraunhofer.iem.spha.model.adapter.CycloneDXVulnerabilityDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.nio.file.Files
import kotlin.io.path.Path
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BlackDuckCycloneDXAdapterTest {

    private fun dtoWith(
        vararg vulns: CycloneDXVulnerabilityDto
    ): CycloneDXDto =
        CycloneDXDto(
            bomFormat = "CycloneDX",
            specVersion = "1.6",
            vulnerabilities = vulns.toList(),
        )

    @Test
    fun testEmptyVulnerabilities() {
        val dto = dtoWith() // no vulnerabilities
        val adapterResult = BlackDuckCycloneDXAdapter.transformDataToKpi(dto)
        assertEquals(0, adapterResult.transformationResults.size)
        assertEquals("Black Duck", assertNotNull(adapterResult.toolInfo).name)
    }

    @Test
    fun testSingleScore() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "CVE-2021-44228",
                    ratings = listOf(CycloneDXRating(score = 6.0, method = "CVSSv31")),
                )
            )

        val result = BlackDuckCycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(1, result.size)
        val first = result.first()
        assertTrue(first is TransformationResult.Success)
        assertEquals(40, (first as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("CVE-2021-44228", first.origin.id)
    }

    @Test
    fun testHighestScoreSelectedAcrossRatings() {
        // Multiple ratings (e.g. NVD + BDSA, CVSSv2 + CVSSv3) -> highest wins.
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "MULTI-RATING",
                    ratings =
                        listOf(
                            CycloneDXRating(score = 5.0, method = "CVSSv2"),
                            CycloneDXRating(score = 8.0, method = "CVSSv31"),
                        ),
                )
            )

        val result = BlackDuckCycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Success)
        // highest = 8.0 -> score 20
        assertEquals(20, (first as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testNoRatingsProducesError() {
        val dto =
            dtoWith(CycloneDXVulnerabilityDto(id = "NO-RATINGS", ratings = emptyList()))

        val result = BlackDuckCycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(1, result.size)
        val first = result.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    @Test
    fun testRatingsWithNullScoresProducesError() {
        // ratings present but no numeric score -> mapNotNull yields empty -> Error
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "NULL-SCORE",
                    ratings = listOf(CycloneDXRating(score = null, severity = "high")),
                )
            )

        val result = BlackDuckCycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    @Test
    fun testBoundaryScoreZeroAndTen() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "ZERO",
                    ratings = listOf(CycloneDXRating(score = 0.0)),
                ),
                CycloneDXVulnerabilityDto(
                    id = "TEN",
                    ratings = listOf(CycloneDXRating(score = 10.0)),
                ),
            )

        val result = BlackDuckCycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(2, result.size)

        val zero = result.first()
        assertTrue(zero is TransformationResult.Success)
        assertEquals(100, (zero as TransformationResult.Success.Kpi).rawValueKpi.score)

        val ten = result.last()
        assertTrue(ten is TransformationResult.Success)
        assertEquals(0, (ten as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testMultipleVulnerabilities() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "VULN-1",
                    ratings = listOf(CycloneDXRating(score = 5.0)),
                ),
                CycloneDXVulnerabilityDto(
                    id = "VULN-2",
                    ratings = listOf(CycloneDXRating(score = 8.0)),
                ),
            )

        val result = BlackDuckCycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(2, result.size)

        val r1 = result.first()
        assertTrue(r1 is TransformationResult.Success)
        assertEquals(50, (r1 as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("VULN-1", r1.origin.id)

        val r2 = result.last()
        assertTrue(r2 is TransformationResult.Success)
        assertEquals(20, (r2 as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("VULN-2", r2.origin.id)
    }

    @Test
    fun testOutOfRangeScoreProducesError() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "OUT-OF-RANGE",
                    ratings = listOf(CycloneDXRating(score = 11.0)),
                )
            )

        val result = BlackDuckCycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}", "{\"bomFormat\":\"CycloneDX\",\"vulnerabilities\":[]}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = BlackDuckCycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
            assertEquals(0, dto.vulnerabilities.count())
        }
    }

    @Test
    fun testResultDto() {
        Files.newInputStream(Path("src/test/resources/blackduck-cyclonedx.json")).use {
            val dto =
                assertDoesNotThrow {
                    BlackDuckCycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
                }
            assertEquals("CycloneDX", dto.bomFormat)
            assertEquals(2, dto.vulnerabilities.count())

            val vuln = dto.vulnerabilities.first()
            assertEquals("CVE-2021-44228", vuln.id)
            assertTrue(vuln.ratings.isNotEmpty())
            assertEquals(10.0, vuln.ratings.first().score)
        }
    }
}