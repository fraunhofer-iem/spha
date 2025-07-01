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
import de.fraunhofer.iem.spha.adapter.KpiAdapter
import de.fraunhofer.iem.spha.model.adapter.RepositoryDetails
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi

object VcsAdapter : KpiAdapter<RepositoryDetails, RepositoryDetails>() {

    override fun transformDataToKpi(
        vararg data: RepositoryDetails
    ): Collection<AdapterResult<RepositoryDetails>> {

        return data.flatMap { repoDetailsDto ->
            return@flatMap listOf(
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.NUMBER_OF_COMMITS.name,
                        score = repoDetailsDto.numberOfCommits,
                    ),
                    origin = repoDetailsDto,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.NUMBER_OF_SIGNED_COMMITS.name,
                        score = repoDetailsDto.numberOfSignedCommits,
                    ),
                    origin = repoDetailsDto,
                ),
                AdapterResult.Success.Kpi(
                    RawValueKpi(
                        typeId = KpiType.IS_DEFAULT_BRANCH_PROTECTED.name,
                        score = if (repoDetailsDto.isDefaultBranchProtected) 100 else 0,
                    ),
                    origin = repoDetailsDto,
                ),
            )
        }
    }
}
