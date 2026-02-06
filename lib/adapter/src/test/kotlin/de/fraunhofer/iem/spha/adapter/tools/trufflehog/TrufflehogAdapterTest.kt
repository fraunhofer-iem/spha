/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trufflehog

import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.TrufflehogResultDto
import de.fraunhofer.iem.spha.model.adapter.TrufflehogFindingDto
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TrufflehogAdapterTest {

    val emptyDto = TrufflehogFindingDto(origins = emptyList())

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
                TrufflehogAdapter.dtoFromJson(it, TrufflehogFindingDto.serializer()),
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = TrufflehogAdapter.dtoFromJson(it, TrufflehogFindingDto.serializer())
            assertEquals(emptyList(), dto.origins)
        }
    }

    @Test
    fun testTransformDataToKpiWithNoSecrets() {
        val dto = TrufflehogFindingDto(origins = emptyList())

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiWithVerifiedSecrets() {
        val dto =
            TrufflehogFindingDto(
                origins =
                    listOf(
                        TrufflehogResultDto(verified = true),
                        TrufflehogResultDto(verified = true),
                        TrufflehogResultDto(verified = false),
                        TrufflehogResultDto(verified = false),
                        TrufflehogResultDto(verified = false),
                    ),
            )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // Single KPI for all findings
        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        // Verified secrets found, so score should be 0
        assertEquals(0, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiWithOnlyUnverifiedSecrets() {
        val dto =
            TrufflehogFindingDto(
                origins =
                    listOf(
                        TrufflehogResultDto(verified = false),
                        TrufflehogResultDto(verified = false),
                    ),
            )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // Single KPI for all findings
        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        // No verified secrets, so score should be 100
        assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiWithEmptyOrigins() {
        val dto = TrufflehogFindingDto(origins = emptyList())

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // Empty origins should result in single KPI with score 100
        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiWithNoSecretsAgain() {
        val dto = TrufflehogFindingDto(origins = emptyList())

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiWithSingleVerifiedSecret() {
        val dto = TrufflehogFindingDto(origins = listOf(TrufflehogResultDto(verified = true)))

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // Single verified secret should create a single KPI with score 0
        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        assertEquals(0, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiToolInfo() {
        val dto = TrufflehogFindingDto(origins = emptyList())

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)

        assertEquals("Trufflehog", adapterResult.toolInfo?.name)
        assertEquals("Secrets Scanner", adapterResult.toolInfo?.description)
    }

    @Test
    fun testTransformDataToKpiMultipleDtos() {
        val dto1 =
            TrufflehogFindingDto(
                origins = listOf(TrufflehogResultDto(verified = false)),
            )
        val dto2 =
            TrufflehogFindingDto(
                origins = listOf(TrufflehogResultDto(verified = true)),
            )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto1, dto2)
        val results = adapterResult.transformationResults

        // Multiple DTOs are combined into a single KPI
        assertEquals(1, results.size)

        // One verified secret found across all DTOs, so score should be 0
        val result = results.first() as TransformationResult.Success.Kpi<*>
        assertEquals(0, result.rawValueKpi.score)
    }
}
