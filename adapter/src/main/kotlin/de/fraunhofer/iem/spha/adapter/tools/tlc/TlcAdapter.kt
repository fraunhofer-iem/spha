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
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.model.adapter.ComponentLag
import de.fraunhofer.iem.spha.model.adapter.TlcDto
import de.fraunhofer.iem.spha.model.adapter.TlcOrigin
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object TlcAdapter : KpiAdapter<TlcDto, TlcOrigin>() {

    override fun transformDataToKpi(vararg data: TlcDto): Collection<AdapterResult<TlcOrigin>> =
        data.flatMap { tlcDto ->
            val baseKpis = createBaseKpis(tlcDto)
            val componentKpis = createComponentKpis(tlcDto)
            baseKpis + componentKpis
        }

    private fun createBaseKpis(tlcDto: TlcDto): List<AdapterResult<TlcOrigin>> {
        val sections =
            listOf(
                tlcDto.transitiveOptional to KpiType.HIGHEST_LIB_DAYS_DEV_TRANSITIVE,
                tlcDto.transitiveProduction to KpiType.HIGHEST_LIB_DAYS_PROD_TRANSITIVE,
                tlcDto.directOptional to KpiType.HIGHEST_LIB_DAYS_DEV_DIRECT,
                tlcDto.directProduction to KpiType.HIGHEST_LIB_DAYS_PROD_DIRECT,
            )

        return sections.map { (tlc, kpiType) ->
            val highestLibyearsComponent = tlc.componentHighestLibdays
            if (highestLibyearsComponent != null) {
                AdapterResult.Success.Kpi(
                    RawValueKpi(typeId = kpiType.name, score = tlc.highestLibdays.toInt()),
                    highestLibyearsComponent,
                )
            } else {
                AdapterResult.Error(ErrorType.DATA_VALIDATION_ERROR)
            }
        }
    }

    private fun createComponentKpis(tlcDto: TlcDto): List<AdapterResult.Success.Kpi<ComponentLag>> {
        val sections =
            listOf(
                tlcDto.transitiveOptional to KpiType.TECHNICAL_LAG_DEV_TRANSITIVE_COMPONENT,
                tlcDto.transitiveProduction to KpiType.TECHNICAL_LAG_PROD_TRANSITIVE_COMPONENT,
                tlcDto.directOptional to KpiType.TECHNICAL_LAG_DEV_DIRECT_COMPONENT,
                tlcDto.directProduction to KpiType.TECHNICAL_LAG_PROD_DIRECT_COMPONENT,
            )

        return sections.flatMap { (tlc, kpiType) ->
            tlc.components.map { compLag -> createComponentKpi(compLag, kpiType) }
        }
    }

    private fun createComponentKpi(
        compLag: ComponentLag,
        kpiType: KpiType,
    ): AdapterResult.Success.Kpi<ComponentLag> =
        AdapterResult.Success.Kpi(
            RawValueKpi(typeId = kpiType.name, score = compLag.technicalLag.libdays.toInt()),
            compLag,
        )
}
