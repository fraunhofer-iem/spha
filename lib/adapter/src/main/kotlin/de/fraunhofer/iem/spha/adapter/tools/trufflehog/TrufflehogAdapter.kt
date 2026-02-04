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
import de.fraunhofer.iem.spha.model.adapter.TrufflehogResultDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object TrufflehogAdapter : KpiAdapter<TrufflehogResultDto, TrufflehogFindingDto>() {

    override fun transformDataToKpi(vararg data: TrufflehogResultDto): AdapterResult<TrufflehogFindingDto> {
        val transformedData =
            data.flatMap { result ->
                val verifiedSecrets = result.verifiedSecrets ?: return@flatMap emptyList()
                val score = if (verifiedSecrets > 0) 0 else 100

                // If there are no findings, create a single KPI with an empty finding placeholder
                if (result.origins.isEmpty()) {
                    listOf(
                        TransformationResult.Success.Kpi(
                            RawValueKpi(score = score, typeId = KpiType.SECRETS.name),
                            origin = TrufflehogFindingDto(verified = false),
                        )
                    )
                } else {
                    result.origins.map { finding ->
                        TransformationResult.Success.Kpi(
                            RawValueKpi(score = score, typeId = KpiType.SECRETS.name),
                            origin = finding,
                        )
                    }
                }
            }

        return AdapterResult(
            toolInfo = ToolInfo(name = "Trufflehog", description = "Secrets Scanner"),
            transformationResults = transformedData,
        )
    }
}
