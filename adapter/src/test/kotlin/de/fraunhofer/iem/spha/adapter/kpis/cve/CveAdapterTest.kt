/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.kpis.cve

import de.fraunhofer.iem.spha.model.kpi.KpiType
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.jupiter.api.Test

class CveAdapterTest {

    @Test
    fun testCreateKpi() {
        // Test with different severity scores
        val kpi1 = createVulnerabilityKpi(score = 0.0, typeId = KpiType.NUMBER_OF_COMMITS)
        assertEquals(100, kpi1?.score)
        assertEquals(KpiType.NUMBER_OF_COMMITS.name, kpi1?.typeId)

        val kpi2 = createVulnerabilityKpi(score = 5.0, typeId = KpiType.NUMBER_OF_COMMITS)
        assertEquals(50, kpi2?.score)

        val kpi3 = createVulnerabilityKpi(score = 10.0, typeId = KpiType.NUMBER_OF_COMMITS)
        assertEquals(0, kpi3?.score)

        // Test with originId
        val originId = "test-origin-id"
        val kpi4 =
            createVulnerabilityKpi(
                score = 7.5,
                typeId = KpiType.NUMBER_OF_COMMITS,
                originId = originId,
            )
        assertEquals(25, kpi4?.score)
        assertEquals(originId, kpi4?.originId)
    }

    @Test
    fun basicVulnerabilityToKpiTransformation() {
        // valid input
        val validKpi = createVulnerabilityKpi(score = 7.5, typeId = KpiType.NUMBER_OF_COMMITS)

        if (validKpi == null || validKpi.score != 25) {
            fail()
        }

        // invalid input
        val invalidKpis =
            listOf(
                createVulnerabilityKpi(score = -7.5, typeId = KpiType.NUMBER_OF_COMMITS),
                createVulnerabilityKpi(score = 11.5, typeId = KpiType.NUMBER_OF_COMMITS),
                createVulnerabilityKpi(score = 10.1, typeId = KpiType.NUMBER_OF_COMMITS),
            )

        invalidKpis.forEach { invalidKpi ->
            if (invalidKpi != null) {
                fail()
            }
        }
    }
}
