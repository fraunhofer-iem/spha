/*
 * Copyright (c) 2024-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TrufflehogNdjsonProcessorTest {

    @Test
    fun testInvalidFileReturnsNull() {
        assertNull(TrufflehogNdjsonProcessor().tryProcess("{\"trufflehog_wrong\": 3}"))
    }

    @Test
    fun testEmptyFileNoSecretsFound() {
        val results = assertDoesNotThrow {
            TrufflehogNdjsonProcessor().tryProcess("")
        }

        assertNotNull(results)

        val kpis = results.transformationResults

        assertEquals(1, kpis.size)
        kpis.forEach { assert(it is TransformationResult.Success) }

        // Empty result, so score should be 100
        assertEquals(100, (kpis.first() as TransformationResult.Success.Kpi).rawValueKpi.score)
    }

    @Test
    fun testNdjsonFormatNoVerifiedSecrets() {
        val content = File("src/test/resources/trufflehog-ndjson.json").readText()

        val results = assertDoesNotThrow {
            TrufflehogNdjsonProcessor().tryProcess(content)
        }

        assertNotNull(results)

        val kpis = results.transformationResults

        // 2 findings = 2 KPIs (one per finding)
        assertEquals(2, kpis.size)
        kpis.forEach { assert(it is TransformationResult.Success) }

        // No verified secrets (both findings have Verified=false), so score should be 100
        kpis.forEach { 
            assertEquals(100, (it as TransformationResult.Success.Kpi).rawValueKpi.score)
        }
    }

    @Test
    fun testNdjsonFormatWithVerifiedSecrets() {
        val content = File("src/test/resources/trufflehog-ndjson-verified.json").readText()

        val results = assertDoesNotThrow {
            TrufflehogNdjsonProcessor().tryProcess(content)
        }

        assertNotNull(results)

        val kpis = results.transformationResults

        // 2 findings = 2 KPIs (one per finding)
        assertEquals(2, kpis.size)
        kpis.forEach { assert(it is TransformationResult.Success) }

        // One verified secret found, so score should be 0 for all KPIs
        kpis.forEach {
            assertEquals(0, (it as TransformationResult.Success.Kpi).rawValueKpi.score)
        }
    }
}
