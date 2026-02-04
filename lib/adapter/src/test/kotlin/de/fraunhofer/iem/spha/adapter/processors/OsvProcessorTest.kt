/*
 * Copyright (c) 2024-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.processors

import de.fraunhofer.iem.spha.adapter.ToolProcessor
import de.fraunhofer.iem.spha.adapter.ToolProcessorStore
import de.fraunhofer.iem.spha.model.adapter.OsvVulnerabilityDto

class OsvProcessorTest : AbstractProcessorTest<OsvVulnerabilityDto>() {

    override fun getProcessor(): ToolProcessor = ToolProcessorStore.processors["osv-scanner"]!!

    override val invalidInputs: List<String>
        get() = listOf(
            "{}",
            "{\"SchemaVersion\": 3}"
        )

    override val validTestResourceFiles: List<String>
        get() = listOf(
            "osv-scanner.json"
        )
}
