/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlinx.serialization.Serializable

@Serializable
data class TlcConfig(val thresholds: Collection<RangeThreshold>) {
    init {
        require(validThresholds(thresholds)) { "TlcConfig ranges are invalid" }
    }

    private fun validThresholds(rangeThresholds: Collection<RangeThreshold>): Boolean {

        val sortedThresholds = rangeThresholds.sortedBy { it.range.from }

        // ranges should be mutually exclusive
        sortedThresholds.forEachIndexed { index, threshold ->
            if (index == sortedThresholds.size - 1) {
                return@forEachIndexed
            }

            val next = sortedThresholds[index + 1]
            if (
                next.range.from != threshold.range.to + 1 ||
                    threshold.range.from > threshold.range.to
            ) {
                return false
            }
        }

        return true
    }
}

@Serializable
data class RangeThreshold(val score: Int, val range: Range) {
    init {
        require(score in 0..100) { "Thresholds must be between 0 and 100" }
    }
}

@Serializable
data class Range(
    // inclusive ranges
    // from: 0 to 20 means that 0 and 20 are in the range
    // and the next range must start at 21
    val from: Long,
    val to: Long,
)
