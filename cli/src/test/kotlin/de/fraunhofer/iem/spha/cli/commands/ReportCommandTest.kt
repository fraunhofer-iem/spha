/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
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
import java.nio.file.FileSystem
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.mock.declare

class ReportCommandTest : KoinTest {
    @JvmField
    @RegisterExtension
    val koinTestRule =
        KoinTestExtension.create {
            printLogger(Level.DEBUG)
            modules(appModules)
        }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testMarkdown_Integration() = runTest {
        val fileSystem =
            declare<FileSystem> { Jimfs.newFileSystem(Configuration.forCurrentPlatform()) }

        fileSystem.provider().createDirectory(fileSystem.getPath("result"))
        fileSystem
            .getPath("./result/kpis.json")
            .writeText(
                "{\"root\":{\"typeId\":\"ROOT\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Incomplete\",\"score\":999,\"reason\":\"Incomplete results.\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"PROCESS_TRANSPARENCY\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"SIGNED_COMMITS_RATIO\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_RATIO_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"NUMBER_OF_COMMITS\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":1.0,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"NUMBER_OF_SIGNED_COMMITS\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":1.0,\"actualWeight\":0.0}]},\"plannedWeight\":1.0,\"actualWeight\":0.0}]},\"plannedWeight\":0.1,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"PROCESS_COMPLIANCE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"CHECKED_IN_BINARIES\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.2,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"SIGNED_COMMITS_RATIO\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_RATIO_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"NUMBER_OF_COMMITS\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":1.0,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"NUMBER_OF_SIGNED_COMMITS\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":1.0,\"actualWeight\":0.0}]},\"plannedWeight\":0.2,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"IS_DEFAULT_BRANCH_PROTECTED\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.3,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"DOCUMENTATION\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"DOCUMENTATION_INFRASTRUCTURE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.6,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"COMMENTS_IN_CODE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.4,\"actualWeight\":0.0}]},\"plannedWeight\":0.3,\"actualWeight\":0.0}]},\"plannedWeight\":0.1,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"SECURITY\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Incomplete\",\"score\":0,\"reason\":\"Incomplete results.\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"SECRETS\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.2,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"MAXIMAL_VULNERABILITY\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":0},\"strategy\":\"MINIMUM_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":10},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.2,\"actualWeight\":0.2},{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":34},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.2,\"actualWeight\":0.2},{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":0},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.2,\"actualWeight\":0.2},{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":14},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.2,\"actualWeight\":0.2},{\"target\":{\"typeId\":\"CODE_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Success\",\"score\":47},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.2,\"actualWeight\":0.2}]},\"plannedWeight\":0.35,\"actualWeight\":1.0},{\"target\":{\"typeId\":\"MAXIMAL_VULNERABILITY\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"MAXIMUM_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"CONTAINER_VULNERABILITY_SCORE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":1.0,\"actualWeight\":0.0}]},\"plannedWeight\":0.35,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"CHECKED_IN_BINARIES\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.1,\"actualWeight\":0.0}]},\"plannedWeight\":0.4,\"actualWeight\":1.0},{\"target\":{\"typeId\":\"INTERNAL_QUALITY\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"DOCUMENTATION\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"DOCUMENTATION_INFRASTRUCTURE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.6,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"COMMENTS_IN_CODE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.4,\"actualWeight\":0.0}]},\"plannedWeight\":1.0,\"actualWeight\":0.0}]},\"plannedWeight\":0.15,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"EXTERNAL_QUALITY\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"DOCUMENTATION\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"WEIGHTED_AVERAGE_STRATEGY\",\"edges\":[{\"target\":{\"typeId\":\"DOCUMENTATION_INFRASTRUCTURE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.6,\"actualWeight\":0.0},{\"target\":{\"typeId\":\"COMMENTS_IN_CODE\",\"result\":{\"type\":\"de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult.Empty\"},\"strategy\":\"RAW_VALUE_STRATEGY\",\"edges\":[]},\"plannedWeight\":0.4,\"actualWeight\":0.0}]},\"plannedWeight\":1.0,\"actualWeight\":0.0}]},\"plannedWeight\":0.25,\"actualWeight\":0.0}]},\"schemaVersion\":\"1.0.0\"}"
            )

        val command = ReportCommand()
        command.test("-r ./result/kpis.json -o ./result/output.md --markdown")

        fileSystem.provider().newInputStream(fileSystem.getPath("./result/output.md")).use {
            val fileContent = it.bufferedReader().readText()
            println(fileContent)
            assertNotEquals("", fileContent)
            assertTrue(fileContent.contains("999"))
        }
    }
}
