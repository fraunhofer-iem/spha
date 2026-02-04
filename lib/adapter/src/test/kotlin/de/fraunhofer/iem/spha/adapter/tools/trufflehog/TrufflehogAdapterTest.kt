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
import de.fraunhofer.iem.spha.model.adapter.TrufflehogFindingDto
import de.fraunhofer.iem.spha.model.adapter.TrufflehogResultDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals

class TrufflehogAdapterTest {

    val emptyDto =
        TrufflehogResultDto(
            verifiedSecrets = null,
            unverifiedSecrets = null,
            origins = emptyList()
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
                TrufflehogAdapter.dtoFromJson(it, TrufflehogResultDto.serializer()),
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = TrufflehogAdapter.dtoFromJson(it, TrufflehogResultDto.serializer())
            assertEquals(null, dto.verifiedSecrets)
        }
    }

    @Test
    fun testTransformDataToKpiWithNoSecrets() {
        val dto = TrufflehogResultDto(
            verifiedSecrets = 0,
            unverifiedSecrets = 0,
            origins = emptyList()
        )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiWithVerifiedSecrets() {
        val dto = TrufflehogResultDto(
            verifiedSecrets = 2,
            unverifiedSecrets = 3,
            origins = listOf(
                TrufflehogFindingDto(verified = true),
                TrufflehogFindingDto(verified = true),
                TrufflehogFindingDto(verified = false),
                TrufflehogFindingDto(verified = false),
                TrufflehogFindingDto(verified = false)
            )
        )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // 5 findings = 5 KPIs (one per finding)
        assertEquals(5, results.size)
        results.forEach { result ->
            assert(result is TransformationResult.Success.Kpi)
            // Verified secrets found, so score should be 0 for all KPIs
            assertEquals(0, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
        }
    }

    @Test
    fun testTransformDataToKpiWithOnlyUnverifiedSecrets() {
        val dto = TrufflehogResultDto(
            verifiedSecrets = 0,
            unverifiedSecrets = 2,
            origins = listOf(
                TrufflehogFindingDto(verified = false),
                TrufflehogFindingDto(verified = false)
            )
        )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // 2 findings = 2 KPIs (one per finding)
        assertEquals(2, results.size)
        results.forEach { result ->
            assert(result is TransformationResult.Success.Kpi)
            // No verified secrets, so score should be 100
            assertEquals(100, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
        }
    }

    @Test
    fun testTransformDataToKpiWithNullVerifiedSecrets() {
        val dto = TrufflehogResultDto(
            verifiedSecrets = null,
            unverifiedSecrets = null,
            origins = emptyList()
        )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // Null verifiedSecrets should result in empty list
        assertEquals(0, results.size)
    }

    @Test
    fun testTransformDataToKpiWithEmptyOriginsButVerifiedSecrets() {
        val dto = TrufflehogResultDto(
            verifiedSecrets = 1,
            unverifiedSecrets = 0,
            origins = emptyList()
        )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)
        val results = adapterResult.transformationResults

        // Empty origins but verifiedSecrets > 0 should create a single KPI with score 0
        assertEquals(1, results.size)
        val result = results.first()
        assert(result is TransformationResult.Success.Kpi)
        assertEquals(0, (result as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testTransformDataToKpiToolInfo() {
        val dto = TrufflehogResultDto(
            verifiedSecrets = 0,
            unverifiedSecrets = 0,
            origins = emptyList()
        )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto)

        assertEquals("Trufflehog", adapterResult.toolInfo?.name)
        assertEquals("Secrets Scanner", adapterResult.toolInfo?.description)
    }

    @Test
    fun testTransformDataToKpiMultipleDtos() {
        val dto1 = TrufflehogResultDto(
            verifiedSecrets = 0,
            unverifiedSecrets = 1,
            origins = listOf(TrufflehogFindingDto(verified = false))
        )
        val dto2 = TrufflehogResultDto(
            verifiedSecrets = 1,
            unverifiedSecrets = 0,
            origins = listOf(TrufflehogFindingDto(verified = true))
        )

        val adapterResult = TrufflehogAdapter.transformDataToKpi(dto1, dto2)
        val results = adapterResult.transformationResults

        // 2 DTOs with 1 finding each = 2 KPIs
        assertEquals(2, results.size)
        
        // First DTO has no verified secrets, score should be 100
        val firstResult = results.first() as TransformationResult.Success.Kpi<*>
        assertEquals(100, firstResult.rawValueKpi.score)
        
        // Second DTO has verified secrets, score should be 0
        val secondResult = results.last() as TransformationResult.Success.Kpi<*>
        assertEquals(0, secondResult.rawValueKpi.score)
    }
}
