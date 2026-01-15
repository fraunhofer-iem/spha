/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.commands

import de.fraunhofer.iem.spha.core.KpiCalculator
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy

interface KpiCalculatorService {
    fun calculateKpis(hierarchy: KpiHierarchy, rawValues: List<RawValueKpi>): KpiResultHierarchy
}

class DefaultKpiCalculatorService : KpiCalculatorService {
    override fun calculateKpis(
        hierarchy: KpiHierarchy,
        rawValues: List<RawValueKpi>,
    ): KpiResultHierarchy {
        return KpiCalculator.calculateKpis(hierarchy, rawValues)
    }
}
