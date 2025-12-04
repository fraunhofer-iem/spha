/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

plugins {
    id("spha-kotlin-conventions")
    application
}

application {
    mainClass.set("de.fraunhofer.iem.spha.cli.MainKt")
}

dependencies {
    implementation(project(":lib:core"))
    implementation(project(":lib:model"))
    implementation(project(":lib:adapter"))
}
