/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trufflehog

import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.TrufflehogReportDto
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TrufflehogAdapterTest {

    val emptyDto =
        TrufflehogReportDto(
            chunks = null,
            bytes = null,
            verifiedSecrets = null,
            unverifiedSecrets = null,
            scanDuration = null,
            trufflehogVersion = null,
        )

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "{\"trufflehog_wrong\": 3}" // Not supported schema
            ]
    )
    fun testInvalidJson(input: String) {
        input.byteInputStream().use {
            assertEquals(
                emptyDto,
                TrufflehogAdapter.dtoFromJson(it, TrufflehogReportDto.serializer()),
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = TrufflehogAdapter.dtoFromJson(it, TrufflehogReportDto.serializer())
            assertEquals(null, dto.verifiedSecrets)
        }
    }

    @Test
    fun testResultDto() {
        Files.newInputStream(Path("src/test/resources/trufflehog-no-result.json")).use {
            val dto = assertDoesNotThrow {
                TrufflehogAdapter.dtoFromJson(it, TrufflehogReportDto.serializer())
            }

            val kpis = assertDoesNotThrow { TrufflehogAdapter.transformDataToKpi(dto) }

            assertEquals(1, kpis.size)

            kpis.forEach { assert(it is TransformationResult.Success) }

            assertEquals(100, (kpis.first() as TransformationResult.Success.Kpi).rawValueKpi.score)
        }
    }

    @Test
    fun testResultResultDto() {
        Files.newInputStream(Path("src/test/resources/trufflehog.json")).use {
            val dto = assertDoesNotThrow {
                TrufflehogAdapter.dtoFromJson(it, TrufflehogReportDto.serializer())
            }

            val kpis = assertDoesNotThrow { TrufflehogAdapter.transformDataToKpi(dto) }

            assertEquals(1, kpis.size)

            kpis.forEach { assert(it is TransformationResult.Success) }

            assertEquals(0, (kpis.first() as TransformationResult.Success.Kpi).rawValueKpi.score)
        }
    }
}
