/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SecObserve observation export, consumed by the quality-gate adapter (B1 KEV, B2 severity).
 *
 * Shape matches SecObserve's paginated observation list (`GET /api/observations/`) reduced to `{
 * "results": [ ...observations... ] }`. Only the fields the gate reads are modeled; the parser
 * ignores unknown keys, so a full observation object deserializes fine. Verified against the
 * SecObserve 1.54.0 OpenAPI schema (`current_severity`, `current_status`, `cve_found_in`).
 *
 * `SecObserveDto` is both a [ToolResult] (it is what the parser deserializes) and an [Origin] (it
 * is attached as the origin of the aggregate KPIs) — mirroring how `TrufflehogResultDto` doubles as
 * both for an aggregate boolean KPI.
 */
@Serializable
data class SecObserveDto(
    @SerialName("results") val results: List<SecObserveObservationDto> = emptyList()
) : ToolResult, Origin

@Serializable
data class SecObserveObservationDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("vulnerability_id") val vulnerabilityId: String? = null,
    // Effective severity/status after parser + rules + assessment (SecObserve "current_*").
    @SerialName("current_severity") val currentSeverity: String? = null,
    @SerialName("current_status") val currentStatus: String? = null,
    // Exploit/KEV annotation: list of { "source": "CISA KEV" | "VulnCheck KEV" | "Exploit-DB" |
    // "PoC GitHub" | "Metasploit" | "Nuclei" }. Sourced from t0sche/cvss-bt via SecObserve's
    // feature_exploit_information. Empty when the CVE is not flagged exploitable.
    @SerialName("cve_found_in") val cveFoundIn: List<SecObserveCveFoundInDto> = emptyList(),
) : Origin

@Serializable data class SecObserveCveFoundInDto(@SerialName("source") val source: String? = null)
