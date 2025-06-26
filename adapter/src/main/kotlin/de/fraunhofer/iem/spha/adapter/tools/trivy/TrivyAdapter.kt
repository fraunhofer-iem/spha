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
import de.fraunhofer.iem.spha.adapter.kpis.cve.CveAdapter
import de.fraunhofer.iem.spha.model.adapter.CVSSData
import de.fraunhofer.iem.spha.model.adapter.TrivyDtoV2
import de.fraunhofer.iem.spha.model.adapter.TrivyVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.VulnerabilityDto
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import kotlin.math.max
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream

object TrivyAdapter : KpiAdapter<TrivyDtoV2, TrivyVulnerabilityDto> {

    private val logger = KotlinLogging.logger {}

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override fun transformDataToKpi(
        data: Collection<TrivyDtoV2>
    ): Collection<AdapterResult<TrivyVulnerabilityDto>> {
        return data
            .flatMap { it.results }
            .flatMap { it.vulnerabilities }
            .flatMap { trivyVuln ->
                val vuln = trivyVulnerabilityToVulnerabilityDto(trivyVuln)
                if (vuln == null) {
                    // Consider adding more context to the error
                    return@flatMap listOf(AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR))
                }

                return@flatMap CveAdapter.transformContainerVulnerabilityToKpi(listOf(vuln)).map {
                    result ->
                    when (result) {
                        is AdapterResult.Success<VulnerabilityDto> -> {
                            AdapterResult.Success.Kpi(result.rawValueKpi, trivyVuln)
                        }
                        is AdapterResult.Error -> result
                    }
                }
            }
    }

    override fun transformDataToKpi(
        data: TrivyDtoV2
    ): Collection<AdapterResult<TrivyVulnerabilityDto>> {
        return transformDataToKpi(listOf(data))
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dtoFromJson(jsonData: InputStream): TrivyDtoV2 {

        return jsonParser.decodeFromStream<TrivyDtoV2>(jsonData)
    }

    /**
     * Transforms a collection of Trivy-specific vulnerabilities into the generalized vulnerability
     * format. Trivy allows annotating multiple CVSS scores to a vulnerability entry (e.g., CVSS2 or
     * CVSS3 or even vendor specific). This transformation always selects the highest available
     * score for each vulnerability.
     */
    private fun trivyVulnerabilityToVulnerabilityDto(
        vulnerability: TrivyVulnerabilityDto
    ): VulnerabilityDto? {
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
        return VulnerabilityDto(
            cveIdentifier = vulnerability.vulnerabilityID,
            packageName = vulnerability.pkgName,
            version = vulnerability.installedVersion,
            severity = score,
        )
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
