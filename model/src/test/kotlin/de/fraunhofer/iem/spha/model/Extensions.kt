/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model

import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode
import kotlin.test.assertEquals

internal fun KpiNode.assertEquals(actual: KpiNode) {
    assertEquals(this.typeId, actual.typeId)
    assertEquals(this.strategy, actual.strategy)
    assertEquals(this.edges, actual.edges)
    assertEquals(this.tags, actual.tags)
    assertEquals(this.reason, actual.reason)
}

internal fun RawValueKpi.assertEquals(actualKpi: RawValueKpi) {
    assertEquals(this.typeId, actualKpi.typeId)
    assertEquals(this.score, actualKpi.score)
    assertEquals(this.id, actualKpi.id)
    assertEquals(this.originId, actualKpi.originId)
}

internal fun KpiResultNode.assertEquals(actualNode: KpiResultNode) {
    assertEquals(this.typeId, actualNode.typeId)
    assertEquals(this.result, actualNode.result)
    assertEquals(this.strategy, actualNode.strategy)
    assertEquals(this.children, actualNode.children)
    assertEquals(this.id, actualNode.id)
    assertEquals(this.tags, actualNode.tags)
    assertEquals(this.originId, actualNode.originId)
    assertEquals(this.reason, actualNode.reason)
}
