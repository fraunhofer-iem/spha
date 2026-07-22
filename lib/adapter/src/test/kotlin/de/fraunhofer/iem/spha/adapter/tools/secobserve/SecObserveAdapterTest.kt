/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.secobserve

import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.SecObserveCveFoundInDto
import de.fraunhofer.iem.spha.model.adapter.SecObserveDto
import de.fraunhofer.iem.spha.model.adapter.SecObserveObservationDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import kotlin.test.Test
import kotlin.test.assertEquals

class SecObserveAdapterTest {

    private fun obs(
        severity: String? = null,
        status: String = "Open",
        foundIn: List<String> = emptyList(),
    ) =
        SecObserveObservationDto(
            currentSeverity = severity,
            currentStatus = status,
            cveFoundIn = foundIn.map { SecObserveCveFoundInDto(it) },
        )

    private fun scoreOf(dto: SecObserveDto, type: KpiType): Int {
        val result = SecObserveAdapter.transformDataToKpi(dto)
        return result.transformationResults
            .filterIsInstance<TransformationResult.Success.Kpi<*>>()
            .first { it.rawValueKpi.typeId == type.name }
            .rawValueKpi
            .score
    }

    @Test
    fun emptyExportPassesBothKpis() {
        val dto = SecObserveDto(results = emptyList())
        assertEquals(100, scoreOf(dto, KpiType.KNOWN_EXPLOITED_VULNERABILITIES))
        assertEquals(100, scoreOf(dto, KpiType.SEVERITY_THRESHOLD_FINDINGS))
    }

    @Test
    fun emitsExactlyTwoKpis() {
        val results = SecObserveAdapter.transformDataToKpi(SecObserveDto()).transformationResults
        assertEquals(2, results.size)
    }

    @Test
    fun criticalOpenFindingFailsB2() {
        val dto = SecObserveDto(results = listOf(obs(severity = "Critical")))
        assertEquals(0, scoreOf(dto, KpiType.SEVERITY_THRESHOLD_FINDINGS))
    }

    @Test
    fun highOpenFindingFailsB2AtDefaultThreshold() {
        val dto = SecObserveDto(results = listOf(obs(severity = "High")))
        assertEquals(0, scoreOf(dto, KpiType.SEVERITY_THRESHOLD_FINDINGS))
    }

    @Test
    fun mediumOpenFindingPassesB2() {
        val dto = SecObserveDto(results = listOf(obs(severity = "Medium")))
        assertEquals(100, scoreOf(dto, KpiType.SEVERITY_THRESHOLD_FINDINGS))
    }

    @Test
    fun suppressedCriticalDoesNotFailB2() {
        val dto =
            SecObserveDto(results = listOf(obs(severity = "Critical", status = "Risk accepted")))
        assertEquals(100, scoreOf(dto, KpiType.SEVERITY_THRESHOLD_FINDINGS))
    }

    @Test
    fun cisaKevOpenFindingFailsB1() {
        val dto =
            SecObserveDto(results = listOf(obs(severity = "Low", foundIn = listOf("CISA KEV"))))
        assertEquals(0, scoreOf(dto, KpiType.KNOWN_EXPLOITED_VULNERABILITIES))
    }

    @Test
    fun vulncheckKevOpenFindingFailsB1() {
        val dto = SecObserveDto(results = listOf(obs(foundIn = listOf("VulnCheck KEV"))))
        assertEquals(0, scoreOf(dto, KpiType.KNOWN_EXPLOITED_VULNERABILITIES))
    }

    @Test
    fun exploitDbButNotKevPassesB1() {
        val dto = SecObserveDto(results = listOf(obs(foundIn = listOf("Exploit-DB", "PoC GitHub"))))
        assertEquals(100, scoreOf(dto, KpiType.KNOWN_EXPLOITED_VULNERABILITIES))
    }

    @Test
    fun suppressedKevDoesNotFailB1() {
        val dto =
            SecObserveDto(
                results = listOf(obs(status = "False positive", foundIn = listOf("CISA KEV")))
            )
        assertEquals(100, scoreOf(dto, KpiType.KNOWN_EXPLOITED_VULNERABILITIES))
    }
}
