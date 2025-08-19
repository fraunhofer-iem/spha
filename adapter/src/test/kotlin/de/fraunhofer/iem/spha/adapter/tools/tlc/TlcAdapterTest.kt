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
import de.fraunhofer.iem.spha.model.adapter.Component
import de.fraunhofer.iem.spha.model.adapter.ComponentLag
import de.fraunhofer.iem.spha.model.adapter.TechnicalLag
import de.fraunhofer.iem.spha.model.adapter.Tlc
import de.fraunhofer.iem.spha.model.adapter.TlcDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class TlcAdapterTest {

    private fun sampleSection(namePrefix: String, scores: List<Double>): Tlc {
        val components =
            scores.mapIndexed { idx, s ->
                ComponentLag(
                    component = Component(bomRef = "$namePrefix-comp-$idx"),
                    technicalLag = TechnicalLag(libdays = s),
                )
            }
        val highest = scores.maxOrNull() ?: 0.0
        return Tlc(
            totalNumComponents = components.size,
            highestLibdays = highest,
            componentHighestLibdays =
                Component(
                    bomRef =
                        "$namePrefix-comp-${scores.indexOfFirst { it == highest }.coerceAtLeast(0)}"
                ),
            components = components,
        )
    }

    private fun sampleDto(): TlcDto {
        return TlcDto(
            transitiveOptional = sampleSection("to", listOf(1.0, 5.0, 3.0)),
            transitiveProduction = sampleSection("tp", listOf(2.0)),
            directOptional = sampleSection("do", listOf()),
            directProduction = sampleSection("dp", listOf(10.0, 7.0)),
        )
    }

    @Test
    fun testModelIntegrity() {
        val tlcDto = sampleDto()

        // Highest libdays non-negative
        assertTrue(tlcDto.transitiveOptional.highestLibdays >= 0.0)
        assertTrue(tlcDto.transitiveProduction.highestLibdays >= 0.0)
        assertTrue(tlcDto.directOptional.highestLibdays >= 0.0)
        assertTrue(tlcDto.directProduction.highestLibdays >= 0.0)

        // componentHighestLibdays bomRef not blank
        assertTrue(tlcDto.transitiveOptional.componentHighestLibdays.bomRef.isNotBlank())
        assertTrue(tlcDto.transitiveProduction.componentHighestLibdays.bomRef.isNotBlank())
        assertTrue(tlcDto.directOptional.componentHighestLibdays.bomRef.isNotBlank())
        assertTrue(tlcDto.directProduction.componentHighestLibdays.bomRef.isNotBlank())

        // totalNumComponents >= components.size
        assertTrue(
            tlcDto.transitiveOptional.totalNumComponents >=
                tlcDto.transitiveOptional.components.size
        )
        assertTrue(
            tlcDto.transitiveProduction.totalNumComponents >=
                tlcDto.transitiveProduction.components.size
        )
        assertTrue(
            tlcDto.directOptional.totalNumComponents >= tlcDto.directOptional.components.size
        )
        assertTrue(
            tlcDto.directProduction.totalNumComponents >= tlcDto.directProduction.components.size
        )
    }

    @Test
    fun testTransformDataToKpi() {
        val tlcDto = sampleDto()

        val kpis = assertDoesNotThrow { TlcAdapter.transformDataToKpi(tlcDto) }

        val componentCount =
            tlcDto.transitiveOptional.components.size +
                tlcDto.transitiveProduction.components.size +
                tlcDto.directOptional.components.size +
                tlcDto.directProduction.components.size
        val expectedTotal = 4 + componentCount
        assertEquals(expectedTotal, kpis.size)

        // All results should be Success.Kpi
        kpis.forEach { assertTrue(it is AdapterResult.Success.Kpi<*>) }

        val kpisTyped = kpis.filterIsInstance<AdapterResult.Success.Kpi<*>>()

        // Verify the 4 base KPIs and their scores equal rounded-down highestLibdays
        fun Double.toScore() = this.toInt()
        val baseMap = kpisTyped.associateBy { it.rawValueKpi.typeId }
        assertEquals(
            tlcDto.transitiveOptional.highestLibdays.toScore(),
            baseMap[KpiType.HIGHEST_LIB_DAYS_DEV_TRANSITIVE.name]?.rawValueKpi?.score,
        )
        assertEquals(
            tlcDto.transitiveProduction.highestLibdays.toScore(),
            baseMap[KpiType.HIGHEST_LIB_DAYS_PROD_TRANSITIVE.name]?.rawValueKpi?.score,
        )
        assertEquals(
            tlcDto.directOptional.highestLibdays.toScore(),
            baseMap[KpiType.HIGHEST_LIB_DAYS_DEV_DIRECT.name]?.rawValueKpi?.score,
        )
        assertEquals(
            tlcDto.directProduction.highestLibdays.toScore(),
            baseMap[KpiType.HIGHEST_LIB_DAYS_PROD_DIRECT.name]?.rawValueKpi?.score,
        )

        // Verify component KPI counts per section
        val typeCounts = kpisTyped.groupingBy { it.rawValueKpi.typeId }.eachCount()
        assertEquals(
            tlcDto.transitiveOptional.components.size,
            typeCounts[KpiType.TECHNICAL_LAG_DEV_TRANSITIVE_COMPONENT.name] ?: 0,
        )
        assertEquals(
            tlcDto.transitiveProduction.components.size,
            typeCounts[KpiType.TECHNICAL_LAG_PROD_TRANSITIVE_COMPONENT.name] ?: 0,
        )
        assertEquals(
            tlcDto.directOptional.components.size,
            typeCounts[KpiType.TECHNICAL_LAG_DEV_DIRECT_COMPONENT.name] ?: 0,
        )
        assertEquals(
            tlcDto.directProduction.components.size,
            typeCounts[KpiType.TECHNICAL_LAG_PROD_DIRECT_COMPONENT.name] ?: 0,
        )
    }

    @Test
    fun testMultipleTransformDataToKpi() {
        val tlcDto1 = sampleDto()
        val tlcDto2 = sampleDto()

        val kpis = assertDoesNotThrow { TlcAdapter.transformDataToKpi(tlcDto1, tlcDto2) }

        val componentCount1 =
            tlcDto1.transitiveOptional.components.size +
                tlcDto1.transitiveProduction.components.size +
                tlcDto1.directOptional.components.size +
                tlcDto1.directProduction.components.size
        val componentCount2 =
            tlcDto2.transitiveOptional.components.size +
                tlcDto2.transitiveProduction.components.size +
                tlcDto2.directOptional.components.size +
                tlcDto2.directProduction.components.size
        val expected = (4 + componentCount1) + (4 + componentCount2)

        assertEquals(expected, kpis.size)

        // All results should be Success.Kpi
        kpis.forEach { assertTrue(it is AdapterResult.Success.Kpi<*>) }
    }

    @Test
    fun testWithRealTechLagData() {
        Files.newInputStream(Path("src/test/resources/techLag-npm-vuejs.json")).use {
            val tlcDto = assertDoesNotThrow { TlcAdapter.dtoFromJson(it, TlcDto.serializer()) }
            
            // Verify basic structure
            assertTrue(tlcDto.transitiveProduction.highestLibdays > 0)
            assertTrue(tlcDto.transitiveProduction.components.isNotEmpty())
            assertTrue(tlcDto.transitiveProduction.componentHighestLibdays.bomRef.isNotBlank())
            
            // Test transformation to KPIs
            val kpis = assertDoesNotThrow { TlcAdapter.transformDataToKpi(tlcDto) }
            
            // All results should be Success.Kpi
            kpis.forEach { assertTrue(it is AdapterResult.Success.Kpi<*>) }
            
            // Should have at least the base KPIs + component KPIs
            val expectedMinimumKpis = 4 + tlcDto.transitiveProduction.components.size +
                    tlcDto.transitiveOptional.components.size +
                    tlcDto.directProduction.components.size +
                    tlcDto.directOptional.components.size
            assertTrue(kpis.size >= expectedMinimumKpis)
        }
    }
}
