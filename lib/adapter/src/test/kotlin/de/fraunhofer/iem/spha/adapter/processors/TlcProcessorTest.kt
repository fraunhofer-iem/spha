/*
 * Copyright (c) 2024-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.processors

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ToolProcessor
import de.fraunhofer.iem.spha.adapter.ToolProcessorStore
import kotlin.test.assertTrue

//class TlcProcessorTest : AbstractProcessorTest() {
//
//    override fun getProcessor(): ToolProcessor = ToolProcessorStore.processors["technicalLag"]!!
//
//    override fun getInvalidInputs(): List<String> = listOf(
//        "{}",
//        "{\"invalid\": true}"
//    )
//
//    override fun getValidTestResourceFiles(): List<String> = listOf(
//        "techLag-npm-vuejs.json",
//        "techLag-npm-angular.json"
//    )
//
//    override fun validateResult(result: AdapterResult<*>, resourceFile: String) {
//        val transformationResults = result.transformationResults
//
//        assertTrue(transformationResults.isNotEmpty(), "Expected non-empty transformation results for $resourceFile")
//    }
//}
