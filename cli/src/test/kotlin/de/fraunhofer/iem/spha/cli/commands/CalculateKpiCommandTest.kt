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
import de.fraunhofer.iem.spha.core.KpiCalculator
import de.fraunhofer.iem.spha.model.kpi.KpiStrategyId
import de.fraunhofer.iem.spha.model.kpi.KpiType
import de.fraunhofer.iem.spha.model.kpi.RawValueKpi
import de.fraunhofer.iem.spha.model.kpi.hierarchy.DefaultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiCalculationResult
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiNode
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultEdge
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultHierarchy
import de.fraunhofer.iem.spha.model.kpi.hierarchy.KpiResultNode
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import java.nio.file.FileSystem
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declare

class CalculateKpiCommandTest : KoinTest {
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

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCalculate_IgnoreIncompatibleFiles() = runTest {
        val fileSystem =
            declare<FileSystem> { Jimfs.newFileSystem(Configuration.forCurrentPlatform()) }

        fileSystem.getPath("./distract.json").writeText("{ \"someKey\" : 123 }")
        fileSystem
            .getPath("./distract.txt")
            .writeText("[{ \"kind\" : \"CHECKED_IN_BINARIES\", \"score\" : 100 }]")

        val expectedResult =
            KpiResultHierarchy.create(
                KpiResultNode(
                    KpiType.ROOT.name,
                    KpiCalculationResult.Success(100),
                    KpiStrategyId.RAW_VALUE_STRATEGY,
                    listOf(),
                )
            )

        mockkObject(KpiCalculator)
        every { KpiCalculator.calculateKpis(DefaultHierarchy.get(), listOf()) } returns
            expectedResult

        val command = CalculateKpiCommand()
        command.test("-o result/h.json")

        fileSystem.provider().newInputStream(fileSystem.getPath("./result/h.json")).use {
            assertEquals(expectedResult, Json.decodeFromStream<KpiResultHierarchy>(it))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCalculate_ReadRawValuesFromFiles() = runTest {
        val fileSystem =
            declare<FileSystem> { Jimfs.newFileSystem(Configuration.forCurrentPlatform()) }

        fileSystem.provider().createDirectory(fileSystem.getPath("tools"))
        fileSystem
            .getPath("./tools/1.json")
            .writeText(
                "[{ \"typeId\" : \"CHECKED_IN_BINARIES\", \"score\" : 100, \"id\": \"\", \"originId\": null}]"
            )
        fileSystem
            .getPath("./tools/2.json")
            .writeText(
                "[{ \"typeId\" : \"SECRETS\", \"score\" : 50, \"id\": \"\", \"originId\": null }]"
            )

        val expectedResult =
            KpiResultHierarchy.create(
                KpiResultNode(
                    KpiType.ROOT.name,
                    KpiCalculationResult.Success(100),
                    KpiStrategyId.RAW_VALUE_STRATEGY,
                    listOf(),
                    id = "",
                )
            )

        mockkObject(KpiCalculator)
        every {
            KpiCalculator.calculateKpis(
                DefaultHierarchy.get(),
                listOf(
                    RawValueKpi(KpiType.CHECKED_IN_BINARIES.name, 100, id = ""),
                    RawValueKpi(KpiType.SECRETS.name, 50, id = ""),
                ),
            )
        } returns expectedResult

        val command = CalculateKpiCommand()
        command.test("-o result.json -s tools")

        fileSystem.provider().newInputStream(fileSystem.getPath("result.json")).use {
            assertEquals(expectedResult, Json.decodeFromStream<KpiResultHierarchy>(it))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCalculate_CustomHierarchy() = runTest {
        val customHierarchy =
            KpiHierarchy.create(
                KpiNode(KpiType.ROOT.name, KpiStrategyId.MAXIMUM_STRATEGY, listOf())
            )

        val fileSystem =
            declare<FileSystem> { Jimfs.newFileSystem(Configuration.forCurrentPlatform()) }

        fileSystem.getPath("./h.json").outputStream().use {
            Json.encodeToStream(customHierarchy, it)
        }

        val expectedResult =
            KpiResultHierarchy.create(
                KpiResultNode(
                    KpiType.ROOT.name,
                    KpiCalculationResult.Empty(),
                    KpiStrategyId.RAW_VALUE_STRATEGY,
                    listOf(),
                    id = "",
                )
            )

        mockkObject(KpiCalculator)
        every { KpiCalculator.calculateKpis(customHierarchy, listOf()) } returns expectedResult

        val command = CalculateKpiCommand()
        command.test("-o result.json -h h.json")

        fileSystem.provider().newInputStream(fileSystem.getPath("result.json")).use {
            assertEquals(expectedResult, Json.decodeFromStream<KpiResultHierarchy>(it))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCalculate_Integration() = runTest {
        val fileSystem =
            declare<FileSystem> { Jimfs.newFileSystem(Configuration.forCurrentPlatform()) }

        fileSystem.provider().createDirectory(fileSystem.getPath("tools"))
        fileSystem
            .getPath("./tools/1.json")
            .writeText(
                "[{ \"typeId\" : \"CHECKED_IN_BINARIES\", \"score\" : 100, \"id\": \"\", \"originId\": null }]"
            )
        fileSystem
            .getPath("./tools/2.json")
            .writeText(
                "[{ \"typeId\" : \"SECRETS\", \"score\" : 50, \"id\": \"\", \"originId\": null }]"
            )

        val command = CalculateKpiCommand()
        command.test("-o result.json -s tools")

        fileSystem.provider().newInputStream(fileSystem.getPath("result.json")).use {
            assertTrue(
                compareKpiResultHierarchies(
                    KpiCalculator.calculateKpis(
                        DefaultHierarchy.get(),
                        listOf(
                            RawValueKpi(KpiType.CHECKED_IN_BINARIES.name, 100, id = ""),
                            RawValueKpi(KpiType.SECRETS.name, 50, id = ""),
                        ),
                    ),
                    Json.decodeFromStream<KpiResultHierarchy>(it),
                )
            )
        }
    }

    /**
     * Compares two [KpiResultHierarchy] objects for equality, ignoring the `id` field within each
     * [KpiResultNode].
     *
     * This function provides a deep comparison of the structure and values of the two hierarchies.
     *
     * @param h1 The first [KpiResultHierarchy] to compare.
     * @param h2 The second [KpiResultHierarchy] to compare.
     * @return `true` if the two hierarchies are structurally and valuewise identical (ignoring node
     *   IDs), `false` otherwise.
     */
    fun compareKpiResultHierarchies(h1: KpiResultHierarchy, h2: KpiResultHierarchy): Boolean {
        // Check for instance equality first for performance.
        if (h1 === h2) return true

        // Compare schema versions.
        if (h1.schemaVersion != h2.schemaVersion) return false

        // Start the recursive comparison from the root nodes.
        return compareNodes(h1.root, h2.root)
    }

    /** Recursively compares two [KpiResultNode] objects, ignoring their `id` fields. */
    private fun compareNodes(n1: KpiResultNode, n2: KpiResultNode): Boolean {
        // Check for instance equality.
        if (n1 === n2) return true

        // Compare all fields except 'id' and 'children'.
        if (
            n1.typeId != n2.typeId ||
                n1.result != n2.result ||
                n1.strategy != n2.strategy ||
                n1.originId != n2.originId
        ) {
            return false
        }

        // Compare the children lists.
        if (n1.edges.size != n2.edges.size) return false

        // Compare each edge in the children list.
        // Using a zip ensures we compare pairs of edges. Since we already checked sizes, this is
        // safe.
        return n1.edges.zip(n2.edges).all { (edge1, edge2) -> compareEdges(edge1, edge2) }
    }

    /** Compares two [KpiResultEdge] objects. */
    private fun compareEdges(e1: KpiResultEdge, e2: KpiResultEdge): Boolean {
        // Check for instance equality.
        if (e1 === e2) return true

        // Compare weights.
        if (e1.plannedWeight != e2.plannedWeight || e1.actualWeight != e2.actualWeight) {
            return false
        }

        // Recursively compare the target nodes.
        return compareNodes(e1.target, e2.target)
    }
}
