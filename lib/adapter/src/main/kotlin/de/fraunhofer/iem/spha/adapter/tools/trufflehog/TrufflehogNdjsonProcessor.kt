/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trufflehog

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ToolProcessor
import de.fraunhofer.iem.spha.model.adapter.TrufflehogFindingDto
import de.fraunhofer.iem.spha.model.adapter.TrufflehogResultDto
import kotlinx.serialization.SerializationException

/**
 * A specialized processor for TruffleHog's NDJSON (newline-delimited JSON) output format. Each line
 * in the file is a separate JSON object representing a finding.
 */
internal class TrufflehogNdjsonProcessor : ToolProcessor {
    private val serializer = TrufflehogFindingDto.serializer()

    override val name: String
        get() = serializer.descriptor.serialName

    override val id: String = ID

    override fun tryProcess(content: String): AdapterResult<*>? {
        val lines = content.lines().filter { it.isNotBlank() }

        val findings = mutableListOf<TrufflehogFindingDto>()

        for (line in lines) {
            try {
                val finding = ToolProcessor.Companion.jsonParser.decodeFromString(serializer, line)
                findings.add(finding)
            } catch (_: SerializationException) {
                return null
            }
        }

        val reportDto =
            TrufflehogResultDto(
                verifiedSecrets = findings.count { it.verified },
                unverifiedSecrets = findings.count { !it.verified },
                origins = findings,
            )

        return TrufflehogAdapter.transformDataToKpi(reportDto)
    }

    companion object {
        const val ID = "trufflehog"
    }
}