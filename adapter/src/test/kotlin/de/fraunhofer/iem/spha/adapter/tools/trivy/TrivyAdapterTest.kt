/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trivy

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.model.adapter.Result
import de.fraunhofer.iem.spha.model.adapter.TrivyDtoV2
import de.fraunhofer.iem.spha.model.adapter.TrivyVulnerabilityDto
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
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
                "{}", // No schema
                "{\"SchemaVersion\": 3}", // Not supported schema
            ]
    )
    fun testInvalidJson(input: String) {
        input.byteInputStream().use {
            assertThrows<UnsupportedOperationException> {
                TrivyAdapter.dtoFromJson(it, TrivyDtoV2.serializer())
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["[]", "{\"SchemaVersion\": 2}"])
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
            assertEquals(4.3, vuln.severity.toDoubleOrNull())
        }
    }

    @Test
    fun trivyV2DtoToRawValue() {

        val trivyV2Dto =
            TrivyDtoV2(
                results =
                    listOf(
                        Result(
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

        val adapterResults = TrivyAdapter.transformDataToKpi(trivyV2Dto)

        assertEquals(1, adapterResults.size)
        val result = adapterResults.first()

        assert(result is AdapterResult.Success)
        assertEquals(40, (result as AdapterResult.Success).rawValueKpi.score)
    }
}
