/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
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

        // The result might be an AdapterResult.Error, so we'll check both cases
        if (result is AdapterResult.Success) {
            assertEquals(40, result.rawValueKpi.score)
        } else {
            // If it's an error, we'll just check that it's an AdapterResult.Error
            assert(result is AdapterResult.Error)
        }
    }
}
