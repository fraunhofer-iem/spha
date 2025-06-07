/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import org.gradle.accessors.dm.LibrariesForLibs

private val Project.libs: LibrariesForLibs
    get() = extensions.getByType()

plugins { `kotlin-dsl` }

repositories {
    mavenCentral()
    // Allow resolving external plugins from precompiled script plugins.
    gradlePluginPortal()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.plugin.dokkatoo)
    implementation(libs.plugin.dokkatoo.javadoc)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.ktfmt)
}
