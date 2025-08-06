/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.model.adapter.TlcDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class TlcAdapterTest {

    @Test
    fun testParsingTlcJson() {
        Files.newInputStream(Path("src/test/resources/techLag-npm-vuejs.json")).use { inputStream ->
            val tlcDto = assertDoesNotThrow {
                TlcAdapter.dtoFromJson(inputStream, TlcDto.serializer())
            }

            // Verify the parsed data matches expected values
            assertEquals<Double>(7092.976631944444, tlcDto.optional.libdays)
            assertEquals<Int>(118, tlcDto.optional.missedReleases)
            assertEquals<Int>(52, tlcDto.optional.numComponents)
            assertEquals<Double>(2707.638275462963, tlcDto.optional.highestLibdays)
            assertEquals<Int>(10, tlcDto.optional.highestMissedReleases)
            assertEquals<String>(
                "@types/node@24.0.7",
                tlcDto.optional.componentHighestMissedReleases.bomRef,
            )
            assertEquals<String>(
                "detect-libc@1.0.3",
                tlcDto.optional.componentHighestLibdays.bomRef,
            )

            assertEquals<Double>(2129.9822916666662, tlcDto.production.libdays)
            assertEquals<Int>(43, tlcDto.production.missedReleases)
            assertEquals<Int>(29, tlcDto.production.numComponents)

            assertEquals<Double>(376.57055555555553, tlcDto.directOptional.libdays)
            assertEquals<Int>(26, tlcDto.directOptional.missedReleases)
            assertEquals<Int>(8, tlcDto.directOptional.numComponents)

            assertEquals(34.48798611111111, tlcDto.directProduction.libdays)
            assertEquals<Int>(1, tlcDto.directProduction.missedReleases)
            assertEquals<Int>(6, tlcDto.directProduction.numComponents)
        }
    }

    @Test
    fun testTransformDataToKpi() {
        Files.newInputStream(Path("src/test/resources/techLag-npm-vuejs.json")).use { inputStream ->
            val tlcDto = TlcAdapter.dtoFromJson(inputStream, TlcDto.serializer())

            val kpis = assertDoesNotThrow { TlcAdapter.transformDataToKpi(tlcDto) }

            // We should have 16 KPIs (4 categories × 4 metrics)
            assertEquals(16, kpis.size)

            // All results should be Success.Kpi - use the same generic type
            kpis.forEach { assertTrue(it is AdapterResult.Success.Kpi<*>) }

            // Verify specific KPIs
            val kpiMap =
                kpis.filterIsInstance<AdapterResult.Success.Kpi<*>>().associateBy {
                    it.rawValueKpi.typeId
                }

            // Check LIB_DAYS metrics
            assertEquals(7092, kpiMap[KpiType.LIB_DAYS_DEV.name]?.rawValueKpi?.score)
            assertEquals(2129, kpiMap[KpiType.LIB_DAYS_PROD.name]?.rawValueKpi?.score)
            assertEquals(376, kpiMap[KpiType.LIB_DAYS_DIRECT_DEV.name]?.rawValueKpi?.score)
            assertEquals(34, kpiMap[KpiType.LIB_DAYS_DIRECT_PROD.name]?.rawValueKpi?.score)

            // Check MISSED_RELEASES metrics
            assertEquals(118, kpiMap[KpiType.LIB_DAYS_MISSED_RELEASES_DEV.name]?.rawValueKpi?.score)
            assertEquals(43, kpiMap[KpiType.LIB_DAYS_MISSED_RELEASES_PROD.name]?.rawValueKpi?.score)
            assertEquals(
                26,
                kpiMap[KpiType.LIB_DAYS_MISSED_RELEASES_DIRECT_DEV.name]?.rawValueKpi?.score,
            )
            assertEquals(
                1,
                kpiMap[KpiType.LIB_DAYS_MISSED_RELEASES_DIRECT_PROD.name]?.rawValueKpi?.score,
            )

            // Check HIGHEST_LIB_DAYS metrics
            assertEquals(2707, kpiMap[KpiType.HIGHEST_LIB_DAYS_DEV.name]?.rawValueKpi?.score)
            assertEquals(787, kpiMap[KpiType.HIGHEST_LIB_DAYS_PROD.name]?.rawValueKpi?.score)
            assertEquals(117, kpiMap[KpiType.HIGHEST_LIB_DAYS_DIRECT_DEV.name]?.rawValueKpi?.score)
            assertEquals(34, kpiMap[KpiType.HIGHEST_LIB_DAYS_DIRECT_PROD.name]?.rawValueKpi?.score)

            // Check HIGHEST_MISSED_RELEASES metrics
            assertEquals(
                10,
                kpiMap[KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_DEV.name]?.rawValueKpi?.score,
            )
            assertEquals(
                19,
                kpiMap[KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_PROD.name]?.rawValueKpi?.score,
            )
            assertEquals(
                10,
                kpiMap[KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_DIRECT_DEV.name]?.rawValueKpi?.score,
            )
            assertEquals(
                1,
                kpiMap[KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_DIRECT_PROD.name]
                    ?.rawValueKpi
                    ?.score,
            )
        }
    }

    @Test
    fun testMultipleTransformDataToKpi() {
        // Create two separate input streams to avoid the issue with reading from the same stream
        // twice
        val tlcDto1 =
            Files.newInputStream(Path("src/test/resources/techLag-npm-vuejs.json")).use {
                inputStream1 ->
                TlcAdapter.dtoFromJson(inputStream1, TlcDto.serializer())
            }

        val tlcDto2 =
            Files.newInputStream(Path("src/test/resources/techLag-npm-vuejs.json")).use {
                inputStream2 ->
                TlcAdapter.dtoFromJson(inputStream2, TlcDto.serializer())
            }

        val kpis = assertDoesNotThrow { TlcAdapter.transformDataToKpi(tlcDto1, tlcDto2) }

        // We should have 32 KPIs (2 DTOs × 16 KPIs each)
        assertEquals(32, kpis.size)

        // All results should be Success.Kpi
        kpis.forEach { assertTrue(it is AdapterResult.Success.Kpi) }
    }
}
