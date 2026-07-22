/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.sbom

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
 * Computes **B3** [KpiType.SBOM_FRESHNESS] from a CycloneDX SBOM's `metadata.timestamp`: 100 if the
 * document is CycloneDX and the timestamp is in the past and within the freshness window, else 0.
 *
 * Fail-closed: a non-CycloneDX document, an absent/unparseable timestamp, or a future timestamp all
 * score 0 — missing SBOM data must never silently pass (this is the in-SPHA equivalent of the
 * interim `gate/sbom-freshness.sh` used on the `calculate` path).
 *
 * Window is `SBOM_FRESHNESS_MAX_AGE_HOURS` (default 24), so the CI job can tune it without CLI
 * changes.
 */
object SbomFreshnessAdapter : KpiAdapter<CycloneDxSbomDto, CycloneDxSbomDto>() {

    private const val DEFAULT_MAX_AGE_HOURS = 24L

    override fun transformDataToKpi(
        vararg data: CycloneDxSbomDto
    ): AdapterResult<CycloneDxSbomDto> {
        val maxAgeHours =
            System.getenv("SBOM_FRESHNESS_MAX_AGE_HOURS")?.trim()?.toLongOrNull()?.takeIf { it > 0 }
                ?: DEFAULT_MAX_AGE_HOURS

        val sbom = data.firstOrNull()
        val score = if (sbom != null && isFresh(sbom, maxAgeHours)) 100 else 0
        logger.info { "SBOM freshness gate (B3): window=${maxAgeHours}h → score=$score" }

        return AdapterResult(
            toolInfo = ToolInfo(name = "CycloneDX SBOM", description = "SBOM freshness"),
            transformationResults =
                listOf(
                    TransformationResult.Success.Kpi(
                        RawValueKpi(typeId = KpiType.SBOM_FRESHNESS.name, score = score),
                        origin = sbom ?: CycloneDxSbomDto(),
                    )
                ),
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
