/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
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
    alias(libs.plugins.semverGit)
    application
}

dependencies {
    implementation(project(":lib:core"))
    implementation(project(":lib:adapter"))
    implementation(project(":lib:model"))
    implementation(libs.bundles.ktor)

    implementation(libs.kotlin.cli)
    implementation(libs.kotlin.logging)

    implementation(libs.slf4j.logger)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.di)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.di.test)
    testImplementation(libs.kotlin.di.junit5)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.fileSystem)
    testImplementation(libs.test.junit5.params)

    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.http.ktor.serialization)
}

application {
    applicationName = "spha-cli"
    mainClass = "de.fraunhofer.iem.spha.cli.MainKt"
}

val sphaCliVersion: String =
    System.getenv("SPHA_CLI_VERSION")
        ?: (findProperty("sphaCliVersion") as String?)
        ?: "0.0.0-dev+local"

version = sphaCliVersion

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> { rejectVersionIf { isNonStable(candidate.version) } }

logger.lifecycle("Building SPHA-CLI version $version.")
