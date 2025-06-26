/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.osv

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.model.adapter.OsvScannerDto
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OsvAdapterTest {
    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "{}", // No schema
                "{\"SchemaVersion\": 3}", // Not supported schema
            ]
    )
    fun testInvalidJson(input: String) {
        input.byteInputStream().use {
            assertThrows<Exception> { OsvAdapter.dtoFromJson(it, OsvScannerDto.serializer()) }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\"results\": []}"])
    fun testEmptyDto(input: String) {
        input.byteInputStream().use {
            val dto = OsvAdapter.dtoFromJson(it, OsvScannerDto.serializer())
            assertEquals(0, dto.results.count())
        }
    }

    @Test
    fun testResultDto() {
        Files.newInputStream(Path("src/test/resources/osv-scanner.json")).use {
            val dto = assertDoesNotThrow { OsvAdapter.dtoFromJson(it, OsvScannerDto.serializer()) }

            val kpis = assertDoesNotThrow { OsvAdapter.transformDataToKpi(dto) }

            assertEquals(8, kpis.size)

            kpis.forEach { assert(it is AdapterResult.Success) }

            val smallest = kpis.minByOrNull { (it as AdapterResult.Success).rawValueKpi.score }!!
            assertEquals(0, (smallest as AdapterResult.Success).rawValueKpi.score)
        }
    }
}
