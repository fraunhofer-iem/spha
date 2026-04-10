/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.fraunhofer.iem.spha.database.UserServiceKey
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.auth.session
import io.ktor.server.response.*
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.directorySessionStorage
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import java.io.File
import kotlinx.serialization.Serializable

@Serializable data class UserSession(val name: String, val count: Int)

fun Application.configureSecurity() {

    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                // TODO: create token blacklist and validate here
                val username = credential.payload.getClaim("username").asString()
                if (!username.isNullOrBlank()) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }

        basic("basic-auth") {
            realm = "SPHA"
            validate { credentials ->
                val userService = application.attributes[UserServiceKey]
                val isValid =
                    userService.verifyUserPassword(
                        username = credentials.name,
                        password = credentials.password,
                    )
                if (isValid) UserIdPrincipal(credentials.name) else null
            }
        }

        session<UserSession>("auth-session") {
            validate { session ->
                if (session.name.isNotBlank()) session else null
            }
            challenge { call.respondRedirect("/login") }
        }
    }

    val cookieSignKey = environment.config.property("cookie.key").getString()

    install(Sessions) {
        val secretSignKey = cookieSignKey.toByteArray()

        // TODO: valid feasibility of session storage directory
        cookie<UserSession>("user_session", directorySessionStorage(File("/tmp/.sessions"))) {
            cookie.secure = true
            cookie.path = "/"
            cookie.maxAgeInSeconds = 120
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }

    routing {

        // ToDo: admin auth later
        authenticate("basic-auth") { post("/api/token/issue") {} }

        authenticate("basic-auth") {
            post("/api/login") {
                val userName = call.principal<UserIdPrincipal>()?.name
                if (userName.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                call.sessions.set(UserSession(name = userName, count = 1))
                call.respondRedirect("/")
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }

    // TODO: verify and implement
    //    val requireHttps =
    //        System.getenv("SPHA_REQUIRE_HTTPS")?.equals("true", ignoreCase = true) ?: true
    //    val allowedMethods = setOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Head)
    //
    //    intercept(ApplicationCallPipeline.Setup) {
    //        if (call.request.httpMethod !in allowedMethods) {
    //            call.respond(HttpStatusCode.MethodNotAllowed)
    //            finish()
    //            return@intercept
    //        }
    //
    //        if (requireHttps) {
    //            val forwardedProto = call.request.headers["X-Forwarded-Proto"]
    //            val scheme = forwardedProto ?: call.request.origin.scheme
    //            if (scheme != "https") {
    //                call.respondRedirect(
    //                    "https://${call.request.host()}${call.request.uri}",
    //                    permanent = true,
    //                )
    //                finish()
    //            }
    //        }
    //    }
}
