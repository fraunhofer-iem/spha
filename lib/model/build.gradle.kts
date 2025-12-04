/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

plugins {
    id("spha-kotlin-conventions")
    alias(libs.plugins.serialization)
}

dependencies {
    implementation(libs.kotlin.serialization.json)
    testImplementation(libs.test.junit5.params)
}
