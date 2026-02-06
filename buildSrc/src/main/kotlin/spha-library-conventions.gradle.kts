/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
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

val sphaLibVersion: String =
    System.getenv("SPHA_LIB_VERSION")
        ?: (findProperty("sphaLibVersion") as String?)
        ?: "0.0.0-dev+local"
version = sphaLibVersion
