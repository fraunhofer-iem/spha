/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.blackduck

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.adapter.kpis.cve.transformVulnerabilityToKpi
import de.fraunhofer.iem.spha.model.adapter.CycloneDXDto
import de.fraunhofer.iem.spha.model.adapter.CycloneDXVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.ToolInfo
import de.fraunhofer.iem.spha.model.kpi.KpiType

object BlackDuckCycloneDXAdapter :
    KpiAdapter<CycloneDXDto, CycloneDXVulnerabilityDto>() {

    override fun transformDataToKpi(
        vararg data: CycloneDXDto
    ): AdapterResult<CycloneDXVulnerabilityDto> {
        require(data.all { it.bomFormat == "CycloneDX" }) {
            "Input is not a CycloneDX document"
        }
        val transformedData =
            data
                .flatMap { it.vulnerabilities }
                .map { vuln ->
                    val score =
                        getHighestCvssScore(vuln)
                            ?: return@map TransformationResult.Error(
                                ErrorType.DATA_VALIDATION_ERROR
                            )
                    val rawValueKpi =
                        transformVulnerabilityToKpi(score, KpiType.CODE_VULNERABILITY_SCORE)
                            ?: return@map TransformationResult.Error(
                                ErrorType.DATA_VALIDATION_ERROR
                            )
                    return@map TransformationResult.Success.Kpi(rawValueKpi, vuln)
                }
        return AdapterResult(
            toolInfo =
                ToolInfo(
                    name = "Black Duck",
                    description = "Software Composition Analysis (CycloneDX SBOM)",
                ),
            transformationResults = transformedData,
        )
    }

    private fun getHighestCvssScore(vulnerability: CycloneDXVulnerabilityDto): Double? {
        // Collect all numeric scores from the ratings array and clamp to the
        // valid CVSS range expected by transformVulnerabilityToKpi (0.0 - 10.0).
        val scores = vulnerability.ratings.mapNotNull { it.score }

        if (scores.isEmpty()) {
            logger.debug {
                "Reported vulnerability '${vulnerability.id}' does not have a score. Skipping!"
            }
            return null
        }

        val highest = scores.max()
        logger.trace {
            "Selected CVSS score $highest for vulnerability '${vulnerability.id}'"
        }
        return highest
    }
}