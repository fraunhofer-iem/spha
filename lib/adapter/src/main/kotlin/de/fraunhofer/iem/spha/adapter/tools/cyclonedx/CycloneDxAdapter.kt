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
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.CycloneDxSbomDto
import de.fraunhofer.iem.spha.model.adapter.ToolInfo
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Adapter for the **CycloneDX SBOM** format (one adapter per format, following `OsvAdapter` /
 * `TrivyAdapter`). It reads a `.cdx.json` document and derives SBOM-level KPIs.
 *
 * Today it emits a single KPI — [KpiType.SBOM_FRESHNESS] (gate B3) — but it is deliberately
 * structured as a format adapter, not a single-KPI adapter: additional SBOM-derived KPIs (component
 * count, presence of a valid `metadata.component`, spec-version checks, …) can be added as further
 * [TransformationResult]s in [transformDataToKpi] without a new adapter or registration.
 *
 * Freshness: 100 if the document is CycloneDX and `metadata.timestamp` is in the past and within
 * the freshness window, else 0 (fail-closed — a non-CycloneDX document, an absent/unparseable
 * timestamp, or a future timestamp all score 0; missing SBOM data must never silently pass). Window
 * is `SBOM_FRESHNESS_MAX_AGE_HOURS` (default 24), so CI can tune it without CLI changes. This is
 * the in-SPHA equivalent of the interim `gate/sbom-freshness.sh` used on the `calculate` path.
 */
object CycloneDxAdapter : KpiAdapter<CycloneDxSbomDto, CycloneDxSbomDto>() {

    private const val DEFAULT_MAX_AGE_HOURS = 24L

    override fun transformDataToKpi(
        vararg data: CycloneDxSbomDto
    ): AdapterResult<CycloneDxSbomDto> {
        val sbom = data.firstOrNull()

        return AdapterResult(
            toolInfo = ToolInfo(name = "CycloneDX", description = "CycloneDX SBOM"),
            transformationResults = listOf(sbomFreshnessKpi(sbom)),
        )
    }

    private fun sbomFreshnessKpi(sbom: CycloneDxSbomDto?): TransformationResult<CycloneDxSbomDto> {
        val maxAgeHours =
            System.getenv("SBOM_FRESHNESS_MAX_AGE_HOURS")?.trim()?.toLongOrNull()?.takeIf { it > 0 }
                ?: DEFAULT_MAX_AGE_HOURS
        val score = if (sbom != null && isFresh(sbom, maxAgeHours)) 100 else 0
        logger.info { "CycloneDX SBOM freshness (B3): window=${maxAgeHours}h → score=$score" }
        return TransformationResult.Success.Kpi(
            RawValueKpi(typeId = KpiType.SBOM_FRESHNESS.name, score = score),
            origin = sbom ?: CycloneDxSbomDto(),
        )
    }

    private fun isFresh(sbom: CycloneDxSbomDto, maxAgeHours: Long): Boolean {
        if (!sbom.bomFormat.equals("CycloneDX", ignoreCase = true)) return false
        val ts = sbom.metadata?.timestamp?.trim().takeUnless { it.isNullOrEmpty() } ?: return false
        val instant = parseInstant(ts) ?: return false
        val now = Instant.now()
        if (instant.isAfter(now)) return false // future timestamp → suspicious, fail-closed
        return instant.isAfter(now.minusSeconds(maxAgeHours * 3600))
    }

    /** Parse an ISO-8601 timestamp, tolerating both `...Z` and explicit-offset forms. */
    private fun parseInstant(ts: String): Instant? =
        runCatching { Instant.parse(ts) }
            .recoverCatching { OffsetDateTime.parse(ts).toInstant() }
            .getOrNull()
}
