/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha

import org.testcontainers.containers.PostgreSQLContainer

object TestDatabaseSetup {
    private val postgresContainer: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:18-alpine").apply {
            start()
            // Set environment variables for the application to use
            System.setProperty("POSTGRES_URL", jdbcUrl)
            System.setProperty("POSTGRES_USER", username)
            System.setProperty("POSTGRES_PASSWORD", password)
        }
    }

    fun setupDatabase() {
        // Access the container to ensure it's started
        postgresContainer
    }
}
