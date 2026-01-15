/*
 * Copyright (c) 2024-2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.github.jmongard.git-semver-plugin") version "0.18.0"
    id("com.github.ben-manes.versions") version "0.53.0"
}

semver {
    // Do not create an empty release commit when running the "releaseVersion" task.
    createReleaseCommit = false

    // Do not let untracked files bump the version or add a "-SNAPSHOT" suffix.
    noDirtyCheck = true
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

tasks.register("printVersions") {
    doLast {
        allprojects.forEach { p ->
            println("${p.path}: version=${p.version}")
        }
    }
}
logger.lifecycle("Building SPHA version $version.")
