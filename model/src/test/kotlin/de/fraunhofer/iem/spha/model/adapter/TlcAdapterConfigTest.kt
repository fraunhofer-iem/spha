/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlin.test.Test
import org.junit.jupiter.api.assertThrows

class TlcAdapterConfigTest {

    @Test
    fun tlcConfigTest() {
        TlcConfig(
            thresholds =
                listOf(
                    RangeThreshold(score = 10, Range(from = 101, to = 110)),
                    RangeThreshold(score = 50, Range(from = 0, to = 100)),
                )
        )
    }

    @Test
    fun tlcInvalidConfigTest() {
        assertThrows<IllegalArgumentException> {
            TlcConfig(thresholds = listOf(RangeThreshold(score = 200, Range(from = 0, to = 100))))
        }

        assertThrows<IllegalArgumentException> {
            TlcConfig(
                thresholds =
                    listOf(
                        RangeThreshold(score = 50, Range(from = 10, to = 50)),
                        RangeThreshold(score = 50, Range(from = 0, to = 100)),
                    )
            )
        }
    }
}
