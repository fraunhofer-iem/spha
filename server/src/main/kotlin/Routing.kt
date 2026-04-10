/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Static plugin. Try to access `/static/index.html`
        singlePageApplication {
            vue("static")
            ignoreFiles { it.endsWith(".gitkeep") }
        }
    }
}

fun Route.authenticatedApiRoutes(build: Route.() -> Unit) {
    authenticate("auth-jwt", "auth-session") { route("/api") { build() } }
}
