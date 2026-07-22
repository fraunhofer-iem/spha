/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.commands

import com.github.ajalt.clikt.command.test
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import de.fraunhofer.iem.spha.cli.appModules
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode
import io.mockk.mockkClass
import java.nio.file.FileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declare

class GateCommandTest : KoinTest {
    @JvmField
    @RegisterExtension
    val koinTestRule =
        KoinTestExtension.create {
            printLogger(Level.DEBUG)
            modules(appModules)
        }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz -> mockkClass(clazz) }

    private fun rootHierarchy(rootResult: KpiCalculationResult): KpiResultHierarchy =
        KpiResultHierarchy.create(
            KpiResultNode("BLOCKING_GATE", rootResult, KpiStrategyId.AND_STRATEGY, emptyList())
        )

    /** Sets up a Jimfs filesystem with a (possibly empty) source dir, and a mocked calculator. */
    private fun setup(result: KpiResultHierarchy): FileSystem {
        val fileSystem =
            declare<FileSystem> { Jimfs.newFileSystem(Configuration.forCurrentPlatform()) }
        fileSystem.provider().createDirectory(fileSystem.getPath("raw"))
        val service = TestKpiCalculatorService { _, _ -> result }
        declare<KpiCalculatorService> { service }
        return fileSystem
    }

    @Test
    fun testGate_passExitsZero() = runTest {
        setup(rootHierarchy(KpiCalculationResult.Success(100)))
        val result = GateCommand().test("-s raw")
        assertEquals(0, result.statusCode)
    }

    @Test
    fun testGate_failExitsNonZero() = runTest {
        setup(rootHierarchy(KpiCalculationResult.Success(0)))
        val result = GateCommand().test("-s raw")
        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("Gate FAILED"))
    }

    @Test
    fun testGate_emptyRootExitsNonZero() = runTest {
        setup(rootHierarchy(KpiCalculationResult.Empty()))
        val result = GateCommand().test("-s raw")
        assertEquals(1, result.statusCode)
    }

    @Test
    fun testGate_unknownRequireNodeExitsNonZero() = runTest {
        setup(rootHierarchy(KpiCalculationResult.Success(100)))
        val result = GateCommand().test("-s raw --require-node NOPE")
        assertEquals(1, result.statusCode)
    }

    @Test
    fun testGate_minScoreBelowThresholdFails() = runTest {
        setup(rootHierarchy(KpiCalculationResult.Success(90)))
        val result = GateCommand().test("-s raw --min-score 100")
        assertEquals(1, result.statusCode)
    }

    @Test
    fun testGate_minScoreMetPasses() = runTest {
        setup(rootHierarchy(KpiCalculationResult.Success(90)))
        val result = GateCommand().test("-s raw --min-score 90")
        assertEquals(0, result.statusCode)
    }

    @Test
    fun testGate_requiresAtLeastOneInput() = runTest {
        setup(rootHierarchy(KpiCalculationResult.Success(100)))
        // Neither -t nor -s → error out, never a silent pass. Mirrors AnalyzeRepositoryCommand's
        // "either --output or --reportUri" contract (IllegalArgumentException → non-zero in
        // main()).
        assertThrows<IllegalArgumentException> { GateCommand().test("") }
    }

    private class TestKpiCalculatorService(
        val handler: (KpiHierarchy, List<RawValueKpi>) -> KpiResultHierarchy
    ) : KpiCalculatorService {
        override fun calculateKpis(
            hierarchy: KpiHierarchy,
            rawValues: List<RawValueKpi>,
        ): KpiResultHierarchy = handler(hierarchy, rawValues)
    }
}
