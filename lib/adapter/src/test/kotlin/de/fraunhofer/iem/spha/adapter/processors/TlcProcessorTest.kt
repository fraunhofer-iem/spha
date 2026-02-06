/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.processors

import de.fraunhofer.iem.spha.adapter.ToolProcessor
import de.fraunhofer.iem.spha.adapter.ToolProcessorStore
import de.fraunhofer.iem.spha.model.adapter.TlcOrigin

class TlcProcessorTest : AbstractProcessorTest<TlcOrigin>() {

    override fun getProcessor(): ToolProcessor = ToolProcessorStore.processors["technicalLag"]!!

    override val invalidInputs: List<String>
        get() = listOf("{}", "{\"invalid\": true}")

    override val validTestResourceFiles: List<String>
        get() = listOf("techLag-npm-vuejs.json", "techLag-npm-angular.json")
}
