/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.processors

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ToolProcessor
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.adapter.tools.trufflehog.TrufflehogNdjsonProcessor
import de.fraunhofer.iem.spha.model.adapter.TrufflehogResultDto
import kotlin.test.assertEquals

class TrufflehogNdjsonProcessorTest : AbstractProcessorTest<TrufflehogResultDto>() {

    override fun getProcessor(): ToolProcessor = TrufflehogNdjsonProcessor()

    override val invalidInputs: List<String>
        get() = listOf("{\"trufflehog_wrong\": 3}")

    override val validTestResourceFiles: List<String>
        get() = listOf("trufflehog-ndjson.json", "trufflehog-ndjson-verified.json")

    override val supportsEmptyInput: Boolean
        get() = true

    override fun validateEmptyInputResult(result: AdapterResult<TrufflehogResultDto>) {
        val kpis = result.transformationResults

        assertEquals(1, kpis.size)
        // Empty result, so score should be 100
        assertEquals(100, (kpis.first() as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    override fun validateResult(result: AdapterResult<TrufflehogResultDto>, resourceFile: String) {
        val kpis = result.transformationResults

        when (resourceFile) {
            "trufflehog-ndjson.json" -> {
                // 2 findings in one file = 1 KPI with two origins
                assertEquals(1, kpis.size)
                // No verified secrets (both findings have Verified=false), so score should be 100
                kpis.forEach {
                    assertEquals(100, (it as TransformationResult.Success.Kpi).rawValueKpi.score)
                    assertEquals(2, it.origin.findings.size)
                }
            }
            "trufflehog-ndjson-verified.json" -> {
                // 2 findings in one file = 1 KPI with two origins
                assertEquals(1, kpis.size)
                // One verified secret found, so score should be 0 for all KPIs
                kpis.forEach {
                    assertEquals(0, (it as TransformationResult.Success.Kpi).rawValueKpi.score)
                    assertEquals(2, it.origin.findings.size)
                }
            }
        }
    }
}
