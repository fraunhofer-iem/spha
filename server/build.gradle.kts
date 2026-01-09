/*
 * Copyright (c) 2024-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("spha-kotlin-conventions")
    alias(libs.plugins.serialization)
    alias(libs.plugins.versions)
    alias(libs.plugins.ktor)
}

group = "de.fraunhofer.iem.spha"

version = "0.0.1"

application {
    applicationName = "spha-server"
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(project(":lib:core"))
    implementation(project(":lib:adapter"))
    implementation(project(":lib:model"))

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.websocket)
    implementation(libs.http.ktor.serialization)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.logger)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.http.ktor.content.negotiation)
    testImplementation(libs.test.container.postgresql)
    testImplementation(libs.test.containers)
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> { rejectVersionIf { isNonStable(candidate.version) } }
