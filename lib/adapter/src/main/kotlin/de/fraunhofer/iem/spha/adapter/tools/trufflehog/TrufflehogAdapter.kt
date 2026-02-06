/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trufflehog

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.ToolInfo
import de.fraunhofer.iem.spha.model.adapter.TrufflehogFindingDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object TrufflehogAdapter : KpiAdapter<TrufflehogFindingDto, TrufflehogFindingDto>() {
    override fun transformDataToKpi(vararg data: TrufflehogFindingDto): AdapterResult<TrufflehogFindingDto> {
        val transformedData = data.flatMap { it.origins }
        val score = if (transformedData.any { it.verified }) 0 else 100
        return AdapterResult(
            toolInfo = ToolInfo(name = "Trufflehog", description = "Secrets Scanner"),
            transformationResults = listOf(
                TransformationResult.Success.Kpi(
                    RawValueKpi(score = score, typeId = KpiType.SECRETS.name),
                    origin = TrufflehogFindingDto(origins = transformedData),
                )
            ),
        )
    }
}
