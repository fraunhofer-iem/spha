/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.trufflehog

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.model.adapter.TrufflehogDto
import de.fraunhofer.iem.spha.model.adapter.TrufflehogReportDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object TrufflehogAdapter : KpiAdapter<TrufflehogDto, TrufflehogReportDto>() {

    override fun transformDataToKpi(
        vararg data: TrufflehogDto
    ): Collection<AdapterResult<TrufflehogReportDto>> {
        return data
            .flatMap { it.results }
            .map {
                val score = if (it.verifiedSecrets > 0) 0 else 100
                AdapterResult.Success.Kpi(
                    RawValueKpi(score = score, typeId = KpiType.SECRETS.name),
                    origin = it,
                )
            }
    }
}
