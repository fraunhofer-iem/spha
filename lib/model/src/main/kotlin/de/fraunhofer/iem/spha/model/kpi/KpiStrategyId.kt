/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.kpi

enum class KpiStrategyId {
    RAW_VALUE_STRATEGY,
    MAXIMUM_STRATEGY,
    MINIMUM_STRATEGY,
    WEIGHTED_AVERAGE_STRATEGY,
    WEIGHTED_MAXIMUM_STRATEGY,
    WEIGHTED_MINIMUM_STRATEGY,
    WEIGHTED_RATIO_STRATEGY,
    XOR_STRATEGY,
    OR_STRATEGY,
    AND_STRATEGY,
}
