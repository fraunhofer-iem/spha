/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi

import de.fraunhofer.iem.spha.model.assertEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RawValueKpiTest {

    @Test
    fun ctorSetsProperties() {
        val rawValueKpi = RawValueKpi("someType", 42, "someKpiId", "someOrigin")
        assertEquals("someType", rawValueKpi.typeId)
        assertEquals(42, rawValueKpi.score)
        assertEquals("someKpiId", rawValueKpi.id)
        assertEquals("someOrigin", rawValueKpi.originId)
    }

    @Test
    fun ctorDefaultProperties() {
        val rawValueKpi = RawValueKpi("someType", 42)
        assertNotNull(rawValueKpi.id)
        assertNull(rawValueKpi.originId)
    }

    @Test
    fun serializeToExpectedJson() {
        val kpis = testRawValueKpis()
        val result = Json.encodeToString(kpis)

        // Only element 0 is expected to have a random generated id. All other entries have preknown
        // IDs.
        val expectedRandomId = kpis[0].id
        val expected =
            "[{\"typeId\":\"someTypeId\",\"score\":42,\"id\":\"$expectedRandomId\"},{\"typeId\":\"someTypeId\",\"score\":42,\"id\":\"someId\",\"originId\":\"someOrigin\"}]"

        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource("testRawValueKpis")
    fun serializeDeserializeRawValueKpi(rawValue: RawValueKpi) {
        val newRawValueKpi = Json.decodeFromString<RawValueKpi>(Json.encodeToString(rawValue))

        rawValue.assertEquals(newRawValueKpi)
    }

    companion object {
        @JvmStatic
        fun testRawValueKpis() =
            listOf(
                RawValueKpi(typeId = "someTypeId", score = 42),
                RawValueKpi(
                    typeId = "someTypeId",
                    score = 42,
                    id = "someId",
                    originId = "someOrigin",
                ),
            )
    }
}
