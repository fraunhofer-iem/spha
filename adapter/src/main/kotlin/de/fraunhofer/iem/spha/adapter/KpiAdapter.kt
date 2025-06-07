/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter

import de.fraunhofer.iem.spha.adapter.tools.tlc.TechLagResult
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

enum class ErrorType {
    DATA_VALIDATION_ERROR
}

sealed class AdapterResult {
    sealed class Success(val rawValueKpi: RawValueKpi) : AdapterResult() {
        class Kpi(rawValueKpi: RawValueKpi) : Success(rawValueKpi)

        class KpiWithResult<T>(rawValueKpi: RawValueKpi, val toolResult: T) : Success(rawValueKpi)

        class KpiTechLag(rawValueKpi: RawValueKpi, val techLag: TechLagResult.Success) :
            Success(rawValueKpi)

        override fun toString(): String {
            return "[Adapter Result Success]: $rawValueKpi"
        }
    }

    data class Error(val type: ErrorType) : AdapterResult()
}
