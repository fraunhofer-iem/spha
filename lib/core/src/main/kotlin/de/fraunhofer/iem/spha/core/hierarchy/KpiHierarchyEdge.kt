/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.core.hierarchy

internal data class KpiHierarchyEdge(
    val to: KpiHierarchyNode,
    val plannedWeight: Double,
    var actualWeight: Double = plannedWeight,
)
