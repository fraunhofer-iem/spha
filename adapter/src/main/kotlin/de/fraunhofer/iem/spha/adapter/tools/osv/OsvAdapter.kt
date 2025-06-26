/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.osv

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.kpis.cve.createVulnerabilityKpi
import de.fraunhofer.iem.spha.model.adapter.OsvScannerDto
import de.fraunhofer.iem.spha.model.adapter.OsvVulnerabilityDto
import de.fraunhofer.iem.spha.model.kpi.KpiType

object OsvAdapter : KpiAdapter<OsvScannerDto, OsvVulnerabilityDto>() {

    override fun transformDataToKpi(
        vararg data: OsvScannerDto
    ): Collection<AdapterResult<OsvVulnerabilityDto>> {
        return data
            .flatMap { it.results }
            .flatMap { it.packages }
            .flatMap { pkg ->
                pkg.vulnerabilities.map { osvVuln ->
                    val score = osvVuln.severity.score.toDoubleOrNull()
                    if (score == null) {
                        return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
                    }
                    val rawValueKpi =
                        createVulnerabilityKpi(score, KpiType.CODE_VULNERABILITY_SCORE)
                    if (rawValueKpi == null) {
                        return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
                    }
                    return@map AdapterResult.Success.Kpi(rawValueKpi, osvVuln)
                }
            }
    }
}
