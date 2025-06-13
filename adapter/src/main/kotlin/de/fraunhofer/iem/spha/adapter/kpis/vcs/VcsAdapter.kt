/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.kpis.vcs

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ErrorType
import de.fraunhofer.iem.spha.model.adapter.vcs.RepositoryDetailsDto
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object VcsAdapter {

    fun transformDataToKpi(
        data: Collection<RepositoryDetailsDto>
    ): Collection<AdapterResult<Unit>> {

        if (data.size != 1) {
            return listOf(AdapterResult.Error(type = ErrorType.DATA_VALIDATION_ERROR))
        }

        // XXX: we need to decide about error handling in adapters
        val repoDetailsDto = data.first()
        return listOf(
            AdapterResult.Success.Kpi(
                RawValueKpi(
                    typeId = KpiType.NUMBER_OF_COMMITS.name,
                    score = repoDetailsDto.numberOfCommits,
                ),
                origin = Unit,
            ),
            AdapterResult.Success.Kpi(
                RawValueKpi(
                    typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                    score = repoDetailsDto.numberOfSignedCommits,
                ),
                origin = Unit,
            ),
            AdapterResult.Success.Kpi(
                RawValueKpi(
                    typeId = KpiType.IS_DEFAULT_BRANCH_PROTECTED.name,
                    score = if (repoDetailsDto.isDefaultBranchProtected) 100 else 0,
                ),
                origin = Unit,
            ),
        )
    }
}
