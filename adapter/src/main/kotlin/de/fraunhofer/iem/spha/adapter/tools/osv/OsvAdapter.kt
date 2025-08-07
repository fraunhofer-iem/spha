/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
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
import de.fraunhofer.iem.spha.adapter.kpis.cve.transformVulnerabilityToKpi
import de.fraunhofer.iem.spha.model.adapter.OsvScannerDto
import de.fraunhofer.iem.spha.model.adapter.OsvVulnerabilityDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import org.metaeffekt.core.security.cvss.CvssVector

object OsvAdapter : KpiAdapter<OsvScannerDto, OsvVulnerabilityDto>() {

    override fun transformDataToKpi(
        vararg data: OsvScannerDto
    ): Collection<AdapterResult<OsvVulnerabilityDto>> {
        return data
            .flatMap { it.results }
            .flatMap { it.packages }
            .flatMap { pkg ->
                pkg.vulnerabilities.map { osvVuln ->
                    // If severity is null or empty, return an error
                    val severityList =
                        osvVuln.severity.mapNotNull { CvssVector.parseVector(it.score)?.baseScore }
                    if (severityList.isEmpty()) {
                        return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
                    }

                    val score =
                        severityList.maxOrNull()
                            ?: return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)

                    val rawValueKpi =
                        transformVulnerabilityToKpi(score, KpiType.CODE_VULNERABILITY_SCORE)
                            ?: return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
                    return@map AdapterResult.Success.Kpi(rawValueKpi, osvVuln)
                }
            }
    }
}
