/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.cyclonedx

import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.CycloneDXDto
import de.fraunhofer.iem.spha.model.adapter.CycloneDXRating
import de.fraunhofer.iem.spha.model.adapter.CycloneDXVulnerabilityDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CycloneDXAdapterTest {

    private fun dtoWith(vararg vulns: CycloneDXVulnerabilityDto): CycloneDXDto =
        CycloneDXDto(bomFormat = "CycloneDX", specVersion = "1.6", vulnerabilities = vulns.toList())

    @Test
    fun testEmptyVulnerabilitiesScoresClean() {
        val dto = dtoWith() // no vulnerabilities
        val adapterResult = CycloneDXAdapter.transformDataToKpi(dto)
        assertEquals("Cyclone DX", assertNotNull(adapterResult.toolInfo).name)

        val result = adapterResult.transformationResults
        assertEquals(1, result.size)
        val only = result.first()
        assertTrue(only is TransformationResult.Success)
        only as TransformationResult.Success.Kpi
        assertEquals(100, only.rawValueKpi.score)
        assertEquals(KpiType.CODE_VULNERABILITY_SCORE.name, only.rawValueKpi.typeId)
        assertNull(only.origin)
    }

    @Test
    fun testUnscoredVulnerabilitiesDoNotScoreClean() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(id = "CVE-2021-44228", ratings = listOf()),
                CycloneDXVulnerabilityDto(id = "CVE-2021-45046", ratings = listOf()),
            )

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults

        assertEquals(2, result.size)
        assertTrue(result.all { it is TransformationResult.Error })
        assertTrue(result.none { it is TransformationResult.Success })
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

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(1, result.size)
        val first = result.first()
        assertTrue(first is TransformationResult.Success)
        assertEquals(40, (first as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("CVE-2021-44228", assertNotNull(first.origin).id)
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

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Success)
        // highest = 8.0 -> score 20
        assertEquals(20, (first as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testNoRatingsProducesError() {
        val dto = dtoWith(CycloneDXVulnerabilityDto(id = "NO-RATINGS", ratings = emptyList()))

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
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

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
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

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
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

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(2, result.size)

        val r1 = result.first()
        assertTrue(r1 is TransformationResult.Success)
        assertEquals(50, (r1 as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("VULN-1", assertNotNull(r1.origin).id)

        val r2 = result.last()
        assertTrue(r2 is TransformationResult.Success)
        assertEquals(20, (r2 as TransformationResult.Success.Kpi).rawValueKpi.score)
        assertEquals("VULN-2", assertNotNull(r2.origin).id)
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

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}", "{\"bomFormat\":\"CycloneDX\",\"vulnerabilities\":[]}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = CycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
            assertEquals(0, dto.vulnerabilities.count())
        }
    }

    @Test
    fun testResultDto() {
        Files.newInputStream(Path("src/test/resources/cyclonedx.json")).use {
            val dto = assertDoesNotThrow {
                CycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
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
