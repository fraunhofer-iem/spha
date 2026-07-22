/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.cyclonedx

import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.CycloneDxMetadataDto
import de.fraunhofer.iem.spha.model.adapter.CycloneDxSbomDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class CycloneDxAdapterTest {

    private fun score(sbom: CycloneDxSbomDto): Int {
        val result = CycloneDxAdapter.transformDataToKpi(sbom)
        val kpi =
            result.transformationResults
                .filterIsInstance<TransformationResult.Success.Kpi<*>>()
                .first()
        assertEquals(KpiType.SBOM_FRESHNESS.name, kpi.rawValueKpi.typeId)
        return kpi.rawValueKpi.score
    }

    private fun sbom(format: String? = "CycloneDX", timestamp: String?) =
        CycloneDxSbomDto(bomFormat = format, metadata = CycloneDxMetadataDto(timestamp = timestamp))

    @Test
    fun freshTimestampPasses() {
        val now = Instant.now().minus(1, ChronoUnit.HOURS).toString()
        assertEquals(100, score(sbom(timestamp = now)))
    }

    @Test
    fun staleTimestampFails() {
        val old = Instant.now().minus(3, ChronoUnit.DAYS).toString()
        assertEquals(0, score(sbom(timestamp = old)))
    }

    @Test
    fun futureTimestampFails() {
        val future = Instant.now().plus(2, ChronoUnit.HOURS).toString()
        assertEquals(0, score(sbom(timestamp = future)))
    }

    @Test
    fun missingTimestampFails() {
        assertEquals(0, score(sbom(timestamp = null)))
    }

    @Test
    fun nonCycloneDxFails() {
        val now = Instant.now().toString()
        assertEquals(0, score(sbom(format = "SPDX", timestamp = now)))
    }

    @Test
    fun offsetTimestampParses() {
        val recentOffset =
            java.time.OffsetDateTime.now()
                .minusHours(2)
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals(100, score(sbom(timestamp = recentOffset)))
    }

    @Test
    fun garbageTimestampFails() {
        assertEquals(0, score(sbom(timestamp = "not-a-date")))
    }
}
