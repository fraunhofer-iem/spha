/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.kpis.cve

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.model.adapter.vulnerability.VulnerabilityDto
import de.fraunhofer.iem.spha.model.kpi.KpiId
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import java.util.*

object CveAdapter {

    fun transformCodeVulnerabilityToKpi(
        data: Collection<VulnerabilityDto>
    ): Collection<AdapterResult> {
        return transformDataToKpi(data, KpiId.CODE_VULNERABILITY_SCORE)
    }

    fun transformContainerVulnerabilityToKpi(
        data: Collection<VulnerabilityDto>
    ): Collection<AdapterResult> {
        return transformDataToKpi(data, KpiId.CONTAINER_VULNERABILITY_SCORE)
    }

    private fun transformDataToKpi(
        data: Collection<VulnerabilityDto>,
        kpiId: KpiId,
    ): Collection<AdapterResult> {
        return data.map {
            return@map if (isValid(it)) {
                AdapterResult.Success.KpiWithResult(
                    RawValueKpi(
                        kpiId = kpiId.name,
                        score = 100 - (it.severity * 10).toInt(),
                        resultId = UUID.randomUUID().toString(),
                        id = UUID.randomUUID().toString(),
                    ),
                    toolResult = it,
                )
            } else {
                AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
            }
        }
    }

    private fun isValid(data: VulnerabilityDto): Boolean {
        return (data.severity in 0.0..10.0 &&
            data.packageName.isNotBlank() &&
            data.cveIdentifier.isNotBlank())
    }
}
