/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

object TlcDefaultConfig {
    fun get(): TlcConfig {
        return TlcConfig(
            listOf(
                RangeThreshold(score = 100, Range(from = 0, to = 60)),
                RangeThreshold(score = 75, Range(from = 61, to = 120)),
                RangeThreshold(score = 50, Range(from = 121, to = 180)),
                RangeThreshold(score = 25, Range(from = 181, to = 360)),
            )
        )
    }
}
