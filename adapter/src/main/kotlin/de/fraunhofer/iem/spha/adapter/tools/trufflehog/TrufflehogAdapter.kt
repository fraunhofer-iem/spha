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
import de.fraunhofer.iem.spha.adapter.ToolInfo
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.TrufflehogReportDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object TrufflehogAdapter : KpiAdapter<TrufflehogReportDto, TrufflehogReportDto>() {

    override fun transformDataToKpi(
        vararg data: TrufflehogReportDto
    ): AdapterResult<TrufflehogReportDto> {
        val transformedData =
            data.mapNotNull {
                val verifiedSecrets = it.verifiedSecrets ?: return@mapNotNull null
                val score = if (verifiedSecrets > 0) 0 else 100
                TransformationResult.Success.Kpi(
                    RawValueKpi(score = score, typeId = KpiType.SECRETS.name),
                    origin = it,
                )
            }

        return AdapterResult(
            toolInfo = ToolInfo(name = "Trufflehog", description = "Secrets Scanner"),
            transformationResults = transformedData,
        )
    }
}
