/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
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
import kotlin.test.assertNotEquals
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
            "[{\"typeId\":\"someTypeId\",\"score\":42,\"id\":\"$expectedRandomId\"},{\"typeId\":\"someTypeId\",\"score\":42,\"originId\":\"someOrigin\",\"id\":\"someId\"}]"

        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource("testRawValueKpis")
    fun serializeDeserializeRawValueKpi(rawValue: RawValueKpi) {
        val newRawValueKpi = Json.decodeFromString<RawValueKpi>(Json.encodeToString(rawValue))

        rawValue.assertEquals(newRawValueKpi)
    }

    @Test
    fun testDefaultConstructor() {
        // When using the default constructor, a random UUID should be generated
        val kpi1 = RawValueKpi("test", 100)
        val kpi2 = RawValueKpi("test", 100)

        // Verify that the IDs are different
        assertNotEquals(kpi1.id, kpi2.id)

        // Verify other properties
        assertEquals("test", kpi1.typeId)
        assertEquals(100, kpi1.score)
        assertEquals(null, kpi1.originId)
    }

    @Test
    fun testCustomIdConstructor() {
        // When using the constructor with a custom ID, that ID should be used
        val customId = "custom-id-123"
        val kpi = RawValueKpi("test", 100, customId, "origin-123")

        // Verify that the custom ID is used
        assertEquals(customId, kpi.id)

        // Verify other properties
        assertEquals("test", kpi.typeId)
        assertEquals(100, kpi.score)
        assertEquals("origin-123", kpi.originId)
    }

    @Test
    fun testEquality() {
        // Two KPIs with the same properties but different IDs should be equal
        // because ID is not part of equals/hashCode
        val kpi1 = RawValueKpi("test", 100, originId = "origin-123")
        val kpi2 = RawValueKpi("test", 100, originId = "origin-123")

        // IDs should be different
        assertNotEquals(kpi1.id, kpi2.id)

        // But the objects should be equal
        assertEquals(kpi1, kpi2)
        assertEquals(kpi1.hashCode(), kpi2.hashCode())
    }

    @Test
    fun testCustomIdEquality() {
        // Two KPIs with the same properties and custom IDs should still be equal
        // because ID is not part of equals/hashCode
        val kpi1 = RawValueKpi("test", 100, "same-id", "origin-123")
        val kpi2 = RawValueKpi("test", 100, "same-id", "origin-123")

        // IDs should be the same
        assertEquals(kpi1.id, kpi2.id)

        // And the objects should be equal
        assertEquals(kpi1, kpi2)
        assertEquals(kpi1.hashCode(), kpi2.hashCode())
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
