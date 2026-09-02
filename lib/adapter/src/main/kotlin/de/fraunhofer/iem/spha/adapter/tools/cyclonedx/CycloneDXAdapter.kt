/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.cyclonedx

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.adapter.kpis.cve.transformVulnerabilityToKpi
import de.fraunhofer.iem.spha.model.adapter.CycloneDXDto
import de.fraunhofer.iem.spha.model.adapter.CycloneDXVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.ToolInfo
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object CycloneDXAdapter : KpiAdapter<CycloneDXDto, CycloneDXVulnerabilityDto>() {

    private const val NO_VULNERABILITY_SCORE = 100
    private val SUPPORTED_SPEC_VERSIONS = setOf("1.4", "1.5", "1.6")

    override fun transformDataToKpi(
        vararg data: CycloneDXDto
    ): AdapterResult<CycloneDXVulnerabilityDto> {
        require(data.all { it.bomFormat == "CycloneDX" }) { "Input is not a CycloneDX document" }
        require(data.all { it.specVersion in SUPPORTED_SPEC_VERSIONS }) {
            "Unsupported CycloneDX specVersion. Supported: $SUPPORTED_SPEC_VERSIONS"
        }

        val vulnerabilities = data.flatMap { it.vulnerabilities }

        val transformedData =
            if (vulnerabilities.isEmpty()) {
                logger.info {
                    "CycloneDX document reports no vulnerabilities. " +
                        "Reporting ${KpiType.CODE_VULNERABILITY_SCORE.name} as $NO_VULNERABILITY_SCORE."
                }
                listOf(
                    TransformationResult.Success.Kpi<CycloneDXVulnerabilityDto>(
                        RawValueKpi(
                            typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                            score = NO_VULNERABILITY_SCORE,
                        )
                    )
                )
            } else {
                vulnerabilities.map { vuln ->
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
            }

        return AdapterResult(
            toolInfo =
                ToolInfo(
                    name = "Cyclone DX",
                    description = "Software Composition Analysis (CycloneDX SBOM)",
                ),
            transformationResults = transformedData,
        )
    }

    private fun getHighestCvssScore(vulnerability: CycloneDXVulnerabilityDto): Double? {
        val scores =
            vulnerability.ratings.mapNotNull { rating ->
                val raw = rating.score ?: rating.severity?.score ?: return@mapNotNull null
                if (raw !in 0.0..10.0) {
                    logger.warn {
                        "Rating for vulnerability '${vulnerability.id}' has out-of-range score $raw. Rejecting!"
                    }
                    return null // reject the whole vulnerability -> DATA_VALIDATION_ERROR
                }
                raw
            }

        if (scores.isEmpty()) {
            logger.debug {
                "Reported vulnerability '${vulnerability.id}' does not have a score or severity. Skipping!"
            }
            return null
        }

        val highest = scores.max()
        logger.trace { "Selected CVSS score $highest for vulnerability '${vulnerability.id}'" }
        return highest
    }
}
