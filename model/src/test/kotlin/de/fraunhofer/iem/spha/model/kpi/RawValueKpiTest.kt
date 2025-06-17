/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RawValueKpiTest {
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
}
