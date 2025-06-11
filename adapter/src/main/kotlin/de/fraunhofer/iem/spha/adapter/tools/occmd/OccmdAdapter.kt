/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.occmd

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.model.adapter.occmd.Checks
import de.fraunhofer.iem.spha.model.adapter.occmd.OccmdDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import java.util.*

object OccmdAdapter {

    fun transformDataToKpi(data: Collection<OccmdDto>): Collection<AdapterResult<Unit>> {

        return data.mapNotNull {
            return@mapNotNull when (Checks.fromString(it.check)) {
                Checks.CheckedInBinaries ->
                    AdapterResult.Success.Kpi(
                        RawValueKpi(
                            typeId = KpiType.CHECKED_IN_BINARIES.name,
                            score = (it.score * 100).toInt(),
                            id = UUID.randomUUID().toString(),
                        ),
                        origin = Unit,
                    )

                Checks.SastUsageBasic ->
                    AdapterResult.Success.Kpi(
                        RawValueKpi(
                            typeId = KpiType.SAST_USAGE.name,
                            score = (it.score * 100).toInt(),
                        ),
                        origin = Unit,
                    )

                Checks.Secrets ->
                    AdapterResult.Success.Kpi(
                        RawValueKpi(
                            typeId = KpiType.SECRETS.name,
                            score = (it.score * 100).toInt(),
                        ),
                        origin = Unit,
                    )

                Checks.CommentsInCode ->
                    AdapterResult.Success.Kpi(
                        RawValueKpi(
                            typeId = KpiType.COMMENTS_IN_CODE.name,
                            score = (it.score * 100).toInt(),
                        ),
                        origin = Unit,
                    )

                Checks.DocumentationInfrastructure ->
                    AdapterResult.Success.Kpi(
                        RawValueKpi(
                            typeId = KpiType.DOCUMENTATION_INFRASTRUCTURE.name,
                            score = (it.score * 100).toInt(),
                        ),
                        origin = Unit,
                    )

                else -> {
                    null
                }
            }
        }
    }
}
