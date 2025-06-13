/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

enum class ErrorType {
    DATA_VALIDATION_ERROR
}

sealed class AdapterResult<out T> {
    /**
     * @param origin describes the data from that the RawValueKpi was created. If, for some reason,
     *   no origin data is available T should be set to Unit.
     */
    sealed class Success<T>(val rawValueKpi: RawValueKpi, val origin: T) : AdapterResult<T>() {
        class Kpi<T>(rawValueKpi: RawValueKpi, origin: T) : Success<T>(rawValueKpi, origin)

        override fun toString(): String {
            return "[Adapter Result Success]: $rawValueKpi"
        }
    }

    data class Error(val type: ErrorType) : AdapterResult<Nothing>()
}
