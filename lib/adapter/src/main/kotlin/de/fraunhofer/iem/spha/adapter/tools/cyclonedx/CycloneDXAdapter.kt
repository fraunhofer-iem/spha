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

    private val toolInfo =
        ToolInfo(
            name = "Cyclone DX",
            description = "Software Composition Analysis (CycloneDX SBOM)",
        )

    override fun transformDataToKpi(
        vararg data: CycloneDXDto
    ): AdapterResult<CycloneDXVulnerabilityDto> {

        val invalidFormat = data.filterNot { it.bomFormat == "CycloneDX" }
        if (invalidFormat.isNotEmpty()) {
            logger.warn { "Input is not a CycloneDX document. Rejecting!" }
            return AdapterResult(
                toolInfo = toolInfo,
                transformationResults =
                    listOf(TransformationResult.Error(ErrorType.DATA_VALIDATION_ERROR)),
            )
        }

        val unsupported = data.map { it.specVersion }.filterNot { it in SUPPORTED_SPEC_VERSIONS }
        if (unsupported.isNotEmpty()) {
            logger.warn {
                "Unsupported CycloneDX specVersion(s) $unsupported. " +
                    "Supported: $SUPPORTED_SPEC_VERSIONS"
            }
            return AdapterResult(
                toolInfo = toolInfo,
                transformationResults =
                    listOf(TransformationResult.Error(ErrorType.DATA_VALIDATION_ERROR)),
            )
        }

        val anyDocumentDeclaresKey = data.any { it.vulnerabilities != null }
        val vulnerabilities = data.flatMap { it.vulnerabilities ?: emptyList() }

        val transformedData =
            when {
                // 2a. no document declares the key -> inventory-only BOM, never scanned.
                //     Emit nothing so the calculator resolves to Empty/Incomplete.
                !anyDocumentDeclaresKey -> {
                    logger.info {
                        "CycloneDX document does not contain a 'vulnerabilities' key. " +
                            "Treating as inventory-only SBOM and reporting no KPI."
                    }
                    emptyList()
                }

                // 2b. key present but empty -> scanned and clean.
                vulnerabilities.isEmpty() -> {
                    logger.info {
                        "CycloneDX document reports no vulnerabilities. " +
                            "Reporting ${KpiType.CODE_VULNERABILITY_SCORE.name} " +
                            "as $NO_VULNERABILITY_SCORE."
                    }
                    listOf(
                        TransformationResult.Success.Kpi<CycloneDXVulnerabilityDto>(
                            RawValueKpi(
                                typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                                score = NO_VULNERABILITY_SCORE,
                            )
                        )
                    )
                }

                // 2c. findings present -> map as before.
                else ->
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

        return AdapterResult(toolInfo = toolInfo, transformationResults = transformedData)
    }

    private fun getHighestCvssScore(vulnerability: CycloneDXVulnerabilityDto): Double? {
        val scores = mutableListOf<Double>()

        for (rating in vulnerability.ratings) {
            val raw = rating.score ?: rating.severity?.score ?: continue
            if (raw !in 0.0..10.0) {
                logger.warn {
                    "Rating for vulnerability '${vulnerability.id}' has out-of-range " +
                        "score $raw. Rejecting the vulnerability!"
                }
                // PR ask 3: reject, do not clamp. Whole vulnerability -> DATA_VALIDATION_ERROR.
                return null
            }
            scores.add(raw)
        }

        if (scores.isEmpty()) {
            logger.debug {
                "Reported vulnerability '${vulnerability.id}' does not have a usable " +
                    "score or severity. Skipping!"
            }
            return null
        }

        return scores.max().also {
            logger.trace { "Selected CVSS score $it for vulnerability '${vulnerability.id}'" }
        }
    }
}
