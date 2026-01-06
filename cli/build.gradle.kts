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
}

application {
    applicationName = "spha-cli"
    mainClass = "de.fraunhofer.iem.spha.cli.MainKt"
}

semver {
    // Do not create an empty release commit when running the "releaseVersion" task.
    createReleaseCommit = false

    // Do not let untracked files bump the version or add a "-SNAPSHOT" suffix.
    noDirtyCheck = true

    groupVersionIncrements = false
}

// Only override a default version (which usually is "unspecified"), but not a custom version.
if (version == Project.DEFAULT_VERSION) {
    version =
        semver.semVersion
            .takeIf { it.isPreRelease }
            // To get rid of a build part's "+" prefix because Docker tags do not support it, use
            // only the original "build"
            // part as the "pre-release" part.
            ?.toString()
            ?.replace("${semver.defaultPreRelease}+", "")
            // Fall back to a plain version without pre-release or build parts.
            ?: semver.version
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> { rejectVersionIf { isNonStable(candidate.version) } }

logger.lifecycle("Building SPHA-CLI version $version.")
