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
import de.fraunhofer.iem.spha.adapter.ToolResultParser
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.CycloneDXDto
import de.fraunhofer.iem.spha.model.adapter.CycloneDXRating
import de.fraunhofer.iem.spha.model.adapter.CycloneDXSeverity
import de.fraunhofer.iem.spha.model.adapter.CycloneDXVulnerabilityDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.MissingFieldException
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class CycloneDXAdapterTest {

    private fun dtoWith(vararg vulns: CycloneDXVulnerabilityDto): CycloneDXDto =
        CycloneDXDto(bomFormat = "CycloneDX", specVersion = "1.6", vulnerabilities = vulns.toList())

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
    fun testRatingsWithNullScoreAndNullSeverityProducesError() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "NULL-SCORE",
                    ratings = listOf(CycloneDXRating(score = null, severity = null)),
                )
            )

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    @Test
    fun testSeverityUsedWhenScoreMissing() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "SEVERITY-ONLY",
                    ratings =
                        listOf(CycloneDXRating(score = null, severity = CycloneDXSeverity.HIGH)),
                )
            )

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Success)
        // HIGH -> 8.0 -> KPI score 20
        assertEquals(20, (first as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testNumericScoreTakesPrecedenceOverSeverity() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "SCORE-WINS",
                    ratings =
                        listOf(CycloneDXRating(score = 2.0, severity = CycloneDXSeverity.CRITICAL)),
                )
            )

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Success)
        // 2.0 used, not CRITICAL's 9.5 -> score 80
        assertEquals(80, (first as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @ParameterizedTest
    @CsvSource("CRITICAL,5", "HIGH,20", "MEDIUM,45", "LOW,80", "INFO,100", "NONE,100")
    fun testSeverityMapping(severity: CycloneDXSeverity, expectedKpiScore: Int) {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "SEV-${severity.name}",
                    ratings = listOf(CycloneDXRating(severity = severity)),
                )
            )

        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        val first = result.first()
        assertTrue(first is TransformationResult.Success)
        assertEquals(
            expectedKpiScore,
            (first as TransformationResult.Success.Kpi).rawValueKpi.score,
        )
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
    @ValueSource(strings = ["{}", "{\"bomFormat\":\"CycloneDX\"}", "{\"specVersion\":\"1.6\"}"])
    fun testMissingRequiredFieldsThrows(input: String) {
        input.byteInputStream().use {
            assertThrows<MissingFieldException> {
                CycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
            }
        }
    }

    @Test
    fun testMissingIdDefaultsToUnknown() {
        """
        {"bomFormat":"CycloneDX","specVersion":"1.6",
        "vulnerabilities":[{"ratings":[{"score":5.0}]}]}
        """
            .trimIndent()
            .byteInputStream()
            .use {
                val dto = CycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
                assertEquals("Unknown", assertNotNull(dto.vulnerabilities).first().id)
            }
    }

    @Test
    fun testResultDto() {
        Files.newInputStream(Path("src/test/resources/cyclonedx.json")).use {
            val dto = assertDoesNotThrow {
                CycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
            }
            assertEquals("CycloneDX", dto.bomFormat)
            assertEquals("1.6", dto.specVersion)

            val vulns = assertNotNull(dto.vulnerabilities)
            assertEquals(2, vulns.count())

            val vuln = vulns.first()
            assertEquals("CVE-2021-44228", vuln.id)
            assertTrue(vuln.ratings.isNotEmpty())
            assertEquals(10.0, vuln.ratings.first().score)
            assertEquals(CycloneDXSeverity.CRITICAL, vuln.ratings.first().severity)
            assertEquals(1, vuln.affects.count())
            assertEquals(
                "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1",
                vuln.affects.first().ref,
            )
        }
    }

    @Test
    fun testAbsentVulnerabilitiesKeyProducesNoKpi() {
        val dto = CycloneDXDto(bomFormat = "CycloneDX", specVersion = "1.6") // key absent -> null
        val adapterResult = CycloneDXAdapter.transformDataToKpi(dto)
        assertEquals("Cyclone DX", assertNotNull(adapterResult.toolInfo).name)
        assertTrue(
            adapterResult.transformationResults.isEmpty(),
            "An inventory-only SBOM must not fabricate a clean score",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["1.4", "1.5", "1.6"])
    fun testSupportedSpecVersions(version: String) {
        // PR ask 1: specVersion is required and gated to the versions this adapter models.
        val dto =
            CycloneDXDto(bomFormat = "CycloneDX", specVersion = version, vulnerabilities = listOf())
        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(1, result.size)
        assertTrue(result.first() is TransformationResult.Success)
    }

    @Test
    fun testEmptyVulnerabilitiesArrayScoresClean() {
        val dto = dtoWith() // key present, empty
        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(1, result.size)
        val only = result.first() as TransformationResult.Success.Kpi
        assertEquals(100, only.rawValueKpi.score)
        assertEquals(KpiType.CODE_VULNERABILITY_SCORE.name, only.rawValueKpi.typeId)
        assertNull(only.origin)
    }

    @Test
    fun testUnrecognisedSeverityIsSkippedButValidFindingSurvivesInProduction(@TempDir dir: Path) {
        val file = dir.resolve("cdx-bogus-sev.json").toFile()
        file.writeText(
            """
            {"bomFormat":"CycloneDX","specVersion":"1.6",
             "vulnerabilities":[
               {"id":"GOOD-1","ratings":[{"score":9.8}]},
               {"id":"BOGUS","ratings":[{"severity":"catastrophic"}]}]}
            """
                .trimIndent()
        )

        // Goes through ToolProcessor.jsonParser, not KpiAdapter.jsonParser.
        val results = ToolResultParser.getAdapterResultsFromJsonFiles(listOf(file))

        assertEquals(1, results.size, "The file must not be dropped as a format mismatch")
        val transformations = results.first().transformationResults
        assertEquals(2, transformations.size)
        assertEquals(1, transformations.count { it is TransformationResult.Success })
        assertEquals(1, transformations.count { it is TransformationResult.Error })
    }

    // COMMENT 4: UNKNOWN no longer scores. Removed from the mapping table.
    @Test
    fun testUnknownSeverityProducesError() {
        val dto =
            dtoWith(
                CycloneDXVulnerabilityDto(
                    id = "SEV-UNKNOWN",
                    ratings = listOf(CycloneDXRating(severity = CycloneDXSeverity.UNKNOWN)),
                )
            )
        val first = CycloneDXAdapter.transformDataToKpi(dto).transformationResults.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    // COMMENT 5: no longer throws, returns a reported validation error instead.
    @Test
    fun testUnsupportedSpecVersionProducesError() {
        val dto = CycloneDXDto(bomFormat = "CycloneDX", specVersion = "1.3")
        val result = CycloneDXAdapter.transformDataToKpi(dto).transformationResults
        assertEquals(1, result.size)
        val first = result.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    @Test
    fun testWrongBomFormatProducesError() {
        val dto = CycloneDXDto(bomFormat = "SPDX", specVersion = "1.6")
        val first = CycloneDXAdapter.transformDataToKpi(dto).transformationResults.first()
        assertTrue(first is TransformationResult.Error)
        assertEquals(ErrorType.DATA_VALIDATION_ERROR, first.type)
    }

    // COMMENT 5: a 1.7 BOM must still be detected as CycloneDX, not dropped.
    @Test
    fun testFutureSpecVersionIsDetectedAndReportsError(@TempDir dir: Path) {
        val file = dir.resolve("cdx-17.json").toFile()
        file.writeText(
            """{"bomFormat":"CycloneDX","specVersion":"1.7",
                "vulnerabilities":[{"id":"CVE-X","ratings":[{"score":9.8}]}]}"""
        )
        val results = ToolResultParser.getAdapterResultsFromJsonFiles(listOf(file))
        assertEquals(1, results.size)
        assertTrue(results.first().transformationResults.all { it is TransformationResult.Error })
    }

    @Test
    fun testMinimalValidDto() {
        """{"bomFormat":"CycloneDX","specVersion":"1.6"}""".byteInputStream().use {
            val dto = CycloneDXAdapter.dtoFromJson(it, CycloneDXDto.serializer())
            assertNull(dto.vulnerabilities) // COMMENT 1: absent, not empty
        }
    }
}
