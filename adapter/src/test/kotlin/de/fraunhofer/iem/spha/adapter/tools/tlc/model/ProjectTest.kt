/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc.model

import de.fraunhofer.iem.spha.model.adapter.ProjectDto
import kotlin.test.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ProjectTest {

    @Test
    fun emptyProject() {
        val projectDto =
            ProjectDto(
                artifacts = listOf(),
                graph = listOf(),
                ecosystem = "",
                version = "",
                artifactId = "",
                groupId = "",
            )
        assertDoesNotThrow { Project.from(projectDto = projectDto) }
    }
}
