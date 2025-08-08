/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.model.adapter.Tlc
import de.fraunhofer.iem.spha.model.adapter.TlcDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

sealed class TechLagResult {
    data class Success(val libyear: Long) : TechLagResult()

    data class Empty(val reason: String) : TechLagResult()
}

object TlcAdapter : KpiAdapter<TlcDto, Tlc>() {

    override fun transformDataToKpi(vararg data: TlcDto): Collection<AdapterResult<Tlc>> {

        return data.flatMap { tlcDto ->
            listOf(
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_DEV.name,
                        score = tlcDto.optional.libdays.toInt(),
                    ),
                    tlcDto.optional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_PROD.name,
                        score = tlcDto.production.libdays.toInt(),
                    ),
                    tlcDto.production,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_DIRECT_DEV.name,
                        score = tlcDto.directOptional.libdays.toInt(),
                    ),
                    tlcDto.directOptional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_DIRECT_PROD.name,
                        score = tlcDto.directProduction.libdays.toInt(),
                    ),
                    tlcDto.directProduction,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_MISSED_RELEASES_DEV.name,
                        score = tlcDto.optional.missedReleases,
                    ),
                    tlcDto.optional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_MISSED_RELEASES_PROD.name,
                        score = tlcDto.production.missedReleases,
                    ),
                    tlcDto.production,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_MISSED_RELEASES_DIRECT_DEV.name,
                        score = tlcDto.directOptional.missedReleases,
                    ),
                    tlcDto.directOptional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.LIB_DAYS_MISSED_RELEASES_DIRECT_PROD.name,
                        score = tlcDto.directProduction.missedReleases,
                    ),
                    tlcDto.directProduction,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_DEV.name,
                        score = tlcDto.optional.highestLibdays.toInt(),
                    ),
                    tlcDto.optional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_DIRECT_DEV.name,
                        score = tlcDto.directOptional.highestLibdays.toInt(),
                    ),
                    tlcDto.directOptional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_PROD.name,
                        score = tlcDto.production.highestLibdays.toInt(),
                    ),
                    tlcDto.production,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_DIRECT_PROD.name,
                        score = tlcDto.directProduction.highestLibdays.toInt(),
                    ),
                    tlcDto.directProduction,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_DEV.name,
                        score = tlcDto.optional.highestMissedReleases,
                    ),
                    tlcDto.optional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_PROD.name,
                        score = tlcDto.production.highestMissedReleases,
                    ),
                    tlcDto.production,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_DIRECT_DEV.name,
                        score = tlcDto.directOptional.highestMissedReleases,
                    ),
                    tlcDto.directOptional,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.HIGHEST_LIB_DAYS_MISSED_RELEASES_DIRECT_PROD.name,
                        score = tlcDto.directProduction.highestMissedReleases,
                    ),
                    tlcDto.directProduction,
                ),
            )
        }
    }
}
