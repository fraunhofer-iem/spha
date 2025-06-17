/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RawValueKpiTest {
    @Test
    fun ctorSetsProperties(){
        val rawValueKpi = RawValueKpi("someType", 42, "someKpiId", "someOrigin")
        assertEquals("someType", rawValueKpi.typeId)
        assertEquals(42, rawValueKpi.score)
        assertEquals("someKpiId", rawValueKpi.id)
        assertEquals("someOrigin", rawValueKpi.originId)
    }

    @Test
    fun ctorDefaultProperties(){
        val rawValueKpi = RawValueKpi("someType", 123)
        assertNotNull(rawValueKpi.id)
        assertNull(rawValueKpi.originId)
    }
}
