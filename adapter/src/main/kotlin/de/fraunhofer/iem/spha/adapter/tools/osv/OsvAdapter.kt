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
import de.fraunhofer.iem.spha.adapter.kpis.cve.CveAdapter
import de.fraunhofer.iem.spha.model.adapter.osv.OsvScannerDto
import de.fraunhofer.iem.spha.model.adapter.vulnerability.VulnerabilityDto
import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

object OsvAdapter {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dtoFromJson(jsonData: InputStream): OsvScannerDto {
        return jsonParser.decodeFromStream<OsvScannerDto>(jsonData)
    }

    fun transformDataToKpi(data: OsvScannerDto): Collection<AdapterResult<VulnerabilityDto>> {
        return transformDataToKpi(listOf(data))
    }

    fun transformDataToKpi(
        data: Collection<OsvScannerDto>
    ): Collection<AdapterResult<VulnerabilityDto>> {
        return CveAdapter.transformCodeVulnerabilityToKpi(
            data.flatMap { results ->
                results.results.flatMap { result ->
                    result.packages.flatMap { pkg ->
                        pkg.groups.mapNotNull { group ->
                            val severity = group.maxSeverity.toDoubleOrNull()
                            if (severity == null) {
                                null
                            } else {
                                VulnerabilityDto(
                                    cveIdentifier = group.ids.first(),
                                    packageName = pkg.osvPackage.name,
                                    severity = severity,
                                    version = pkg.osvPackage.version,
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
