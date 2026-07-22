/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.secobserve

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.SecObserveDto
import de.fraunhofer.iem.spha.model.adapter.SecObserveObservationDto
import de.fraunhofer.iem.spha.model.adapter.ToolInfo
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

/**
 * Turns a SecObserve observation export into the two SecObserve-backed quality-gate KPIs:
 * - **B1** [KpiType.KNOWN_EXPLOITED_VULNERABILITIES] — 0 if any non-suppressed observation is
 *   known-exploited (in CISA KEV or VulnCheck KEV via `cve_found_in`), else 100.
 * - **B2** [KpiType.SEVERITY_THRESHOLD_FINDINGS] — 0 if any non-suppressed observation is at or
 *   over the configured severity threshold, else 100.
 *
 * Both are aggregate booleans over the whole export, so the [SecObserveDto] itself is the origin
 * (mirroring `TrufflehogAdapter`). "Non-suppressed" and the threshold are defined in
 * [SecObserveGateConfig]; fail-closed choices are documented there.
 */
object SecObserveAdapter : KpiAdapter<SecObserveDto, SecObserveDto>() {

    override fun transformDataToKpi(vararg data: SecObserveDto): AdapterResult<SecObserveDto> {
        val observations = data.flatMap { it.results }
        val active = observations.filterNot { SecObserveGateConfig.isSuppressed(it) }

        // B1: zero non-suppressed known-exploited (KEV) findings.
        val kevScore = if (active.any { SecObserveGateConfig.isKnownExploited(it) }) 0 else 100

        // B2: zero non-suppressed findings at/over the severity threshold.
        val threshold = SecObserveGateConfig.severityThreshold()
        val overThreshold =
            active.any { SecObserveGateConfig.severityRank(it.currentSeverity) >= threshold }
        val sevScore = if (overThreshold) 0 else 100

        logger.info {
            "SecObserve gate: ${observations.size} observations (${active.size} non-suppressed) → " +
                "B1(KEV)=$kevScore B2(severity>=${SecObserveGateConfig.thresholdName()})=$sevScore"
        }

        val origin = SecObserveDto(results = observations)
        return AdapterResult(
            toolInfo =
                ToolInfo(name = "SecObserve", description = "SecObserve vulnerability management"),
            transformationResults =
                listOf(
                    TransformationResult.Success.Kpi(
                        RawValueKpi(
                            typeId = KpiType.KNOWN_EXPLOITED_VULNERABILITIES.name,
                            score = kevScore,
                        ),
                        origin = origin,
                    ),
                    TransformationResult.Success.Kpi(
                        RawValueKpi(
                            typeId = KpiType.SEVERITY_THRESHOLD_FINDINGS.name,
                            score = sevScore,
                        ),
                        origin = origin,
                    ),
                ),
        )
    }
}

/**
 * Gate policy for the SecObserve KPIs, tunable via environment variables so the CI job can set them
 * without CLI changes:
 * - `SEVERITY_THRESHOLD` (default `High`) — B2 blocks at/above this SecObserve severity.
 *
 * Severity order follows SecObserve's `SeverityEnum`: Unknown < None < Low < Medium < High <
 * Critical. Unknown/unparseable severities rank lowest (0) so they never *raise* the gate above
 * what the data supports.
 */
internal object SecObserveGateConfig {

    // Ranking of SecObserve severities; higher = more severe. Unknown maps to 0.
    private val severityRanks =
        mapOf("unknown" to 0, "none" to 1, "low" to 2, "medium" to 3, "high" to 4, "critical" to 5)

    // current_status values that mean "triaged away or already fixed" → not counted by the gate.
    // Everything else (Open, Affected, In review, or anything unrecognized) counts — fail-closed.
    private val suppressedStatuses =
        setOf(
            "resolved",
            "duplicate",
            "false positive",
            "not affected",
            "not security",
            "risk accepted",
        )

    fun severityRank(severity: String?): Int = severityRanks[severity?.trim()?.lowercase()] ?: 0

    fun thresholdName(): String =
        System.getenv("SEVERITY_THRESHOLD")?.trim().takeIf { !it.isNullOrEmpty() } ?: "High"

    /** Numeric threshold for B2; an unrecognized override falls back to High (rank 4). */
    fun severityThreshold(): Int = severityRanks[thresholdName().lowercase()] ?: 4

    fun isSuppressed(o: SecObserveObservationDto): Boolean =
        o.currentStatus?.trim()?.lowercase() in suppressedStatuses

    /** True if the observation is flagged as known-exploited via CISA KEV or VulnCheck KEV. */
    fun isKnownExploited(o: SecObserveObservationDto): Boolean =
        o.cveFoundIn.any { it.source?.contains("KEV", ignoreCase = true) == true }
}
