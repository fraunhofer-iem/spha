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
    `java-library`
    id("publish-conventions")
}

java {
    withSourcesJar()
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaGeneratePublicationJavadoc"))
}

var sphaLibVersion: String = rootProject.version.toString()
sphaLibVersion = (
    findProperty("sphaLibVersion") as String?
        ?: System.getenv("SPHA_LIB_VERSION")
) ?: sphaLibVersion
version = sphaLibVersion
