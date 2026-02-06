/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

plugins { id("com.vanniktech.maven.publish") }

group = "de.fraunhofer.iem"

mavenPublishing {
    publishToMavenCentral()
    // Only sign publications when not publishing to mavenLocal
    if (gradle.startParameter.taskNames.none { it.contains("publishToMavenLocal") }) {
        signAllPublications()
    }
    coordinates("de.fraunhofer.iem", "spha-${project.name}")
    pom {
        name = "spha-${project.name}"
        description = "SPHA is a collection of libraries to work with hierarchical KPI models."
        url = "https://github.com/fraunhofer-iem/spha"
        licenses {
            license {
                name = "MIT License"
                url = "https://github.com/fraunhofer-iem/spha/blob/main/LICENSE.md"
            }
        }
        developers {
            developer {
                name = "Jan-Niclas Struewer"
                email = "jan-niclas.struewer@iem.fraunhofer.de"
            }
            developer {
                name = "Sebastian Leuer"
                email = "sebastian.leuer@iem.fraunhofer.de"
            }
        }
        scm {
            connection = "scm:git:git@github.com:fraunhofer-iem/spha.git"
            developerConnection = "scm:git:ssh://github.com:fraunhofer-iem/spha.git"
            url = "https://github.com/fraunhofer-iem/spha"
        }
    }
}
