/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.model.adapter.ProjectDto
import de.fraunhofer.iem.spha.model.adapter.TlcDto
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow

class TlcAdapterNpmIntegrationTest {

    @Test
    fun npmTransformationTest() {

        Files.newInputStream(Path("src/test/resources/tlc-result-npm.json")).use {
            val dto = assertDoesNotThrow { TlcAdapter.dtoFromJson(it, TlcDto.serializer()) }
            val kpis = TlcAdapter.transformDataToKpi(dto)

            assertEquals(2, kpis.size)

            // artifact[0] chart.js prod - 95 days
            //            //used
            //            {
            //                "versionNumber": "4.4.3",
            //                "releaseDate": 1715950573000, // Fri 17 May 2024
            //                "isDefault": false
            //            },
            // target
            //            {
            //                "versionNumber": "4.4.4",
            //                "releaseDate": 1724186056000, // Tue 20 August 2024
            //                "isDefault": true
            //            }

            // artifact [3] js-cookie prod
            // used == target
            //             {
            //              "versionNumber": "3.0.5",
            //              "releaseDate": 1682328231000,
            //              "isDefault": true
            //            }
            //
            // artifact [4] js-token prod - 2202 days
            // used
            //             {
            //              "versionNumber": "4.0.0",
            //              "releaseDate": 1517140738000, // Sun 28 January 2018
            //              "isDefault": false
            //            },
            // target
            //            {
            //              "versionNumber": "5.0.0",
            //              "releaseDate": 1560610465000, // Sat 15 June 2019
            //              "isDefault": false
            //            },
            //             {
            //              "versionNumber": "9.0.0",
            //              "releaseDate": 1707433147000, // Thu 8 February 2024
            //              "isDefault": true
            //            }
            // artifact [5] loose-envify
            //             {
            //              "versionNumber": "1.4.0",
            //              "releaseDate": 1531220985000,
            //              "isDefault": true
            //            }
            // artifact [9] react-chartjs-2
            //             {
            //              "versionNumber": "5.2.0",
            //              "releaseDate": 1673301325000,
            //              "isDefault": true
            //            }
            // artifact [10] react
            //              used
            //            {
            //              "versionNumber": "18.3.1",
            //              "releaseDate": 1714149746000,
            //              "isDefault": true
            //            },
            //              newer versions exist but no stable !
            //             {
            //              "versionNumber": "19.0.0-rc-fb9a90fa48-20240614",
            //              "releaseDate": 1718381559000,
            //              "isDefault": false
            //            }
            // artifact [16] color
            // {
            //   "versionNumber": "0.3.2",
            //  "releaseDate": 1673369513000,
            // "isDefault": true
            // }
            val prodTechLag = kpis.first() as AdapterResult.Success<ProjectDto>
            assertEquals(2297, prodTechLag.rawValueKpi.score)

            // artifact[1] - esbuild - 68 days (due to the release hour)
            // used
            //            {
            //                "versionNumber": "0.21.5",
            //                "releaseDate": 1717967861000, // Sun 9 June 2024
            //                "isDefault": false
            //            },
            // target
            //             {
            //              "versionNumber": "0.23.1",
            //              "releaseDate": 1723846448000, // Sat 17 August 2024
            //              "isDefault": true
            //            }
            // artifact [2] - fsevents
            //             {
            //              "versionNumber": "2.3.3",
            //              "releaseDate": 1692635062000,
            //              "isDefault": true
            //            }
            // artifact [6] - nanoid - 153 days
            //            {
            //              "versionNumber": "3.3.7",
            //              "releaseDate": 1699247731000, // Mon 6 November 2023
            //              "isDefault": false
            //            },
            //
            //            {
            //              "versionNumber": "5.0.7",
            //              "releaseDate": 1712508244000, // Sun 7 April 2024
            //              "isDefault": true
            //            }
            // artifact [7] - picocolors
            //            {
            //              "versionNumber": "1.0.1",
            //              "releaseDate": 1715654656000,
            //              "isDefault": true
            //            }
            // artifact [8] - postcss - 64 days (due to release hour)
            //            {
            //              "versionNumber": "8.4.39",
            //              "releaseDate": 1719685321000, // Sat 29 June 2024
            //              "isDefault": false
            //            },
            //
            //             {
            //              "versionNumber": "8.4.44",
            //              "releaseDate": 1725265415000, // Mon 2 September 2024
            //              "isDefault": true
            //            }
            // artifact [11] - 52 days (due to release hour)
            //            {
            //              "versionNumber": "4.18.1",
            //              "releaseDate": 1720452347000, // Mon 8 July 2024
            //              "isDefault": false
            //            },
            //
            //             {
            //              "versionNumber": "4.21.2",
            //              "releaseDate": 1725001498000, // Fri 30 August 2024
            //              "isDefault": true
            //
            // artifact [12] - source-map-js
            //            {
            //              "versionNumber": "1.2.0",
            //              "releaseDate": 1710865296000,
            //              "isDefault": true
            //            }
            // artifact [13] - typescript -  21 days (due to release hour)
            //            {
            //              "versionNumber": "5.5.3",
            //              "releaseDate": 1719859350000, // Mon 1 July 2024
            //              "isDefault": false
            //            },
            //
            //            {
            //              "versionNumber": "5.5.4",
            //              "releaseDate": 1721689371000, // Tue 23 July 2024
            //              "isDefault": true
            //            },
            // artifact [14] - vite - 48 days
            //            {
            //              "versionNumber": "5.3.3",
            //              "releaseDate": 1719982411000, // Wed 3 July 2024
            //              "isDefault": false
            //            },
            //
            //            {
            //              "versionNumber": "5.4.2",
            //              "releaseDate": 1724163389000, // Tue 20 August 2024
            //              "isDefault": true
            //            },
            // artifact [15] - darwin-arm64 - 68 days (due to the release hour)
            //          {
            //              "versionNumber": "0.21.5",
            //              "releaseDate": 1717967833000, // Sun 9 June 2024
            //              "isDefault": false
            //            },
            //
            //
            //              "versionNumber": "0.23.1",
            //              "releaseDate": 1723846413000, // Sat 17 August 2024
            //              "isDefault": true
            //            }
            // artifact [17] - rollup-darwin-arm64 - 52 days (due to release hour)
            //            {
            //              "versionNumber": "4.18.1",
            //              "releaseDate": 1720452305000, // Mon 8 July 2024
            //              "isDefault": false
            //            },
            //
            //             {
            //              "versionNumber": "4.21.2",
            //              "releaseDate": 1725001460000, // Fri 30 August 2024
            //              "isDefault": true
            //            }
            // artifact [18] - estree
            //          {
            //              "versionNumber": "1.0.5",
            //              "releaseDate": 1699323835000,
            //              "isDefault": true
            //            }
            // artifact [19] - js-cookie
            //            {
            //              "versionNumber": "3.0.6",
            //              "releaseDate": 1699346476000,
            //              "isDefault": true
            //            }
            val devTechLag = kpis.elementAt(1) as AdapterResult.Success<ProjectDto>
            assertEquals(526, devTechLag.rawValueKpi.score)
        }
    }
}
