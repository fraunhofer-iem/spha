/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trivy

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.kpis.cve.createVulnerabilityKpi
import de.fraunhofer.iem.spha.model.adapter.CVSSData
import de.fraunhofer.iem.spha.model.adapter.TrivyDtoV2
import de.fraunhofer.iem.spha.model.adapter.TrivyVulnerabilityDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import kotlin.math.max
import kotlinx.serialization.json.decodeFromJsonElement

object TrivyAdapter : KpiAdapter<TrivyDtoV2, TrivyVulnerabilityDto>() {

    override fun transformDataToKpi(
        vararg data: TrivyDtoV2
    ): Collection<AdapterResult<TrivyVulnerabilityDto>> {
        return data
            .flatMap { it.results }
            .flatMap { it.vulnerabilities }
            .map { trivyVuln ->
                val score = getHighestCvssScore(trivyVuln)
                if (score == null) {
                    return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
                }
                val rawValueKpi =
                    createVulnerabilityKpi(score, KpiType.CONTAINER_VULNERABILITY_SCORE)
                if (rawValueKpi == null) {
                    return@map AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
                }
                return@map AdapterResult.Success.Kpi(rawValueKpi, trivyVuln)
            }
    }

    private fun getHighestCvssScore(vulnerability: TrivyVulnerabilityDto): Double? {
        if (vulnerability.cvss == null) {
            logger.debug {
                "Reported vulnerability '${vulnerability.vulnerabilityID}' does not have a score. Skipping!"
            }
            return null
        }

        val cvssData =
            vulnerability.cvss!!.values.map { jsonParser.decodeFromJsonElement<CVSSData>(it) }

        val score = getHighestCvssScore(cvssData)
        logger.trace {
            "Selected CVSS score $score for vulnerability '${vulnerability.vulnerabilityID}'"
        }
        return score
    }

    private fun getHighestCvssScore(scores: Collection<CVSSData>): Double {
        // NB: If no value was coded, we simply return 0.0 (no vulnerability)
        // In practice, this should never happen
        var v2Score = 0.0
        var v3Score = 0.0

        for (data in scores) {
            if (data.v2Score != null) v2Score = max(v2Score, data.v2Score!!)

            if (data.v3Score != null) v3Score = max(v3Score, data.v3Score!!)
        }

        return max(v2Score, v3Score)
    }
}
