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
import de.fraunhofer.iem.spha.adapter.kpis.cve.CveAdapter
import de.fraunhofer.iem.spha.model.adapter.OsvScannerDto
import de.fraunhofer.iem.spha.model.adapter.OsvVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.VulnerabilityDto
import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

object OsvAdapter : KpiAdapter<OsvScannerDto, OsvVulnerabilityDto> {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dtoFromJson(jsonData: InputStream): OsvScannerDto {
        return jsonParser.decodeFromStream<OsvScannerDto>(jsonData)
    }

    override fun transformDataToKpi(
        data: OsvScannerDto
    ): Collection<AdapterResult<OsvVulnerabilityDto>> {
        return transformDataToKpi(listOf(data))
    }

    override fun transformDataToKpi(
        data: Collection<OsvScannerDto>
    ): Collection<AdapterResult<OsvVulnerabilityDto>> {
        return data
            .flatMap { it.results }
            .flatMap { it.packages }
            .flatMap { pkg ->
                pkg.vulnerabilities.flatMap { osvVuln ->
                    val severity = osvVuln.severity.score.toDoubleOrNull()
                    if (severity == null) {
                        // Handle validation error for a single group, following the new pattern.
                        // Adding context to the error is a good practice.
                        return@flatMap listOf(AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR))
                    }

                    // Create the DTO for the valid group.
                    val vuln =
                        VulnerabilityDto(
                            cveIdentifier = osvVuln.id,
                            packageName = pkg.osvPackage.name,
                            severity = severity,
                            version = pkg.osvPackage.version,
                        )

                    // Call the adapter for the single DTO. `flatMap` will correctly un-nest the
                    // returned collection of adapter results.
                    return@flatMap CveAdapter.transformContainerVulnerabilityToKpi(listOf(vuln))
                        .map { result ->
                            when (result) {
                                is AdapterResult.Success<VulnerabilityDto> -> {
                                    AdapterResult.Success.Kpi(result.rawValueKpi, osvVuln)
                                }
                                is AdapterResult.Error -> result
                            }
                        }
                }
            }
    }
}
