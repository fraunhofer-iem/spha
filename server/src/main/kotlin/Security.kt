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

fun Application.configureSecurity() {
    // ToDo: Setup jwt security
    // Please read the jwt property from the config file if you are using EngineMain
    //    val jwtAudience = "jwt-audience"
    //    val jwtDomain = "https://jwt-provider-domain/"
    //    val jwtRealm = "ktor sample app"
    //    val jwtSecret = "secret"
    //    authentication {
    //        jwt {
    //            realm = jwtRealm
    //            verifier(
    //                JWT
    //                    .require(Algorithm.HMAC256(jwtSecret))
    //                    .withAudience(jwtAudience)
    //                    .withIssuer(jwtDomain)
    //                    .build()
    //            )
    //            validate { credential ->
    //                if (credential.payload.audience.contains(jwtAudience))
    // JWTPrincipal(credential.payload) else null
    //            }
    //        }
    //    }
}
