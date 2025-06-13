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
import de.fraunhofer.iem.spha.adapter.kpis.cve.CveAdapter
import de.fraunhofer.iem.spha.model.adapter.trivy.CVSSData
import de.fraunhofer.iem.spha.model.adapter.trivy.TrivyDto
import de.fraunhofer.iem.spha.model.adapter.trivy.TrivyDtoV1
import de.fraunhofer.iem.spha.model.adapter.trivy.TrivyDtoV2
import de.fraunhofer.iem.spha.model.adapter.trivy.TrivyVulnerabilityDto
import de.fraunhofer.iem.spha.model.adapter.vulnerability.VulnerabilityDto
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import kotlin.math.max
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object TrivyAdapter {

    private val logger = KotlinLogging.logger {}

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun transformDataToKpi(
        data: Collection<TrivyDto>
    ): Collection<AdapterResult<VulnerabilityDto>> {
        return CveAdapter.transformContainerVulnerabilityToKpi(data.flatMap { it.vulnerabilities })
    }

    fun transformDataToKpi(data: TrivyDto): Collection<AdapterResult<VulnerabilityDto>> {
        return transformDataToKpi(listOf(data))
    }

    fun transformTrivyV2ToKpi(
        data: Collection<TrivyDtoV2>
    ): Collection<AdapterResult<VulnerabilityDto>> {
        // TODO: better origin types
        return CveAdapter.transformContainerVulnerabilityToKpi(
            createVulnerabilitiesDto(data.flatMap { it.results.flatMap { it.vulnerabilities } })
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dtoFromJson(jsonData: InputStream): TrivyDto {
        val json = Json.decodeFromStream<JsonElement>(jsonData)

        if (json is JsonArray) return parseV1(json)
        else if (json !is JsonObject)
            throw UnsupportedOperationException("The provided Trivy result is not supported.")

        val schemaVersion = json["SchemaVersion"]?.jsonPrimitive?.intOrNull

        if (schemaVersion == 2) return parseV2(json)

        throw UnsupportedOperationException(
            "Trivy results for schema version '$schemaVersion' are currently not supported."
        )
    }

    private fun parseV1(json: JsonArray): TrivyDto {
        logger.info { "Processing Trivy result from version 0.19.0 or earlier." }
        val v1dto = jsonParser.decodeFromJsonElement<List<TrivyDtoV1>>(json)
        val vulnerabilities = createVulnerabilitiesDto(v1dto.flatMap { it.vulnerabilities })
        return TrivyDto(vulnerabilities)
    }

    private fun parseV2(json: JsonObject): TrivyDto {
        logger.info { "Processing Trivy result of SchemaVersion: 2" }
        val v2dto = jsonParser.decodeFromJsonElement<TrivyDtoV2>(json)
        val vulnerabilities = createVulnerabilitiesDto(v2dto.results.flatMap { it.vulnerabilities })
        return TrivyDto(vulnerabilities)
    }

    /**
     * Transforms a collection of Trivy-specific vulnerabilities into the generalized vulnerability
     * format. Trivy allows annotating multiple CVSS scores to a vulnerability entry (e.g., CVSS2 or
     * CVSS3 or even vendor specific). This transformation always selects the highest available
     * score for each vulnerability.
     */
    private fun createVulnerabilitiesDto(
        vulnerabilities: Collection<TrivyVulnerabilityDto>
    ): Collection<VulnerabilityDto> {
        return vulnerabilities.mapNotNull {
            if (it.cvss == null) {
                logger.debug {
                    "Reported vulnerability '${it.vulnerabilityID}' does not have a score. Skipping!"
                }
                return@mapNotNull null
            }

            val cvssData = it.cvss!!.values.map { jsonParser.decodeFromJsonElement<CVSSData>(it) }

            val score = getHighestCvssScore(cvssData)
            logger.trace { "Selected CVSS score $score for vulnerability '${it.vulnerabilityID}'" }
            VulnerabilityDto(
                cveIdentifier = it.vulnerabilityID,
                packageName = it.pkgName,
                version = it.installedVersion,
                severity = score,
            )
        }
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
