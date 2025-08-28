/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
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
import de.fraunhofer.iem.spha.adapter.ToolInfo
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.adapter.kpis.cve.transformVulnerabilityToKpi
import de.fraunhofer.iem.spha.model.adapter.CVSSData
import de.fraunhofer.iem.spha.model.adapter.TrivyDtoV2
import de.fraunhofer.iem.spha.model.adapter.TrivyVulnerabilityDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import kotlin.math.max
import kotlinx.serialization.json.decodeFromJsonElement

object TrivyAdapter : KpiAdapter<TrivyDtoV2, TrivyVulnerabilityDto>() {

    override fun transformDataToKpi(vararg data: TrivyDtoV2): AdapterResult<TrivyVulnerabilityDto> {
        val transformedData =
            data
                .flatMap { it.results }
                .flatMap { it.vulnerabilities }
                .map { trivyVuln ->
                    val score =
                        getHighestCvssScore(trivyVuln)
                            ?: return@map TransformationResult.Error(
                                ErrorType.DATA_VALIDATION_ERROR
                            )
                    val rawValueKpi =
                        transformVulnerabilityToKpi(score, KpiType.CONTAINER_VULNERABILITY_SCORE)
                            ?: return@map TransformationResult.Error(
                                ErrorType.DATA_VALIDATION_ERROR
                            )
                    return@map TransformationResult.Success.Kpi(rawValueKpi, trivyVuln)
                }
        return AdapterResult(
            toolInfo = ToolInfo(name = "Trivy", description = "Container Image Scanner"),
            transformationResults = transformedData,
        )
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
