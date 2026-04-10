/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.database

import io.ktor.util.AttributeKey
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.sql.Statement
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger

val UserServiceKey = AttributeKey<UserService>("UserService")

class UserService(private val connection: Connection, private val log: Logger) {
    companion object {
        private const val CREATE_TABLE_USERS =
            """CREATE TABLE IF NOT EXISTS USERS (
                ID SERIAL PRIMARY KEY,
                USERNAME VARCHAR(255) UNIQUE NOT NULL,
                PASSWORD_HASH BYTEA NOT NULL,
                SALT BYTEA NOT NULL,
                ITERATIONS INT NOT NULL,
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );"""

        private const val SELECT_USER_BY_USERNAME =
            "SELECT PASSWORD_HASH, SALT, ITERATIONS FROM USERS WHERE USERNAME = ?"

        private const val INSERT_USER =
            "INSERT INTO USERS (USERNAME, PASSWORD_HASH, SALT, ITERATIONS) VALUES (?, ?, ?, ?)"

        private const val DEFAULT_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_USERS)
    }

    suspend fun verifyUserPassword(username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val selectStmt = connection.prepareStatement(SELECT_USER_BY_USERNAME)
            selectStmt.setString(1, username)
            val resultSet = selectStmt.executeQuery()
            if (!resultSet.next()) {
                return@withContext false
            }

            val storedHash = resultSet.getBytes("PASSWORD_HASH")
            val salt = resultSet.getBytes("SALT")
            val iterations = resultSet.getInt("ITERATIONS")

            val computedHash =
                hashPassword(
                    password = password.toCharArray(),
                    salt = salt,
                    iterations = iterations,
                )
            return@withContext MessageDigest.isEqual(storedHash, computedHash)
        }

    suspend fun createUser(username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val salt = ByteArray(SALT_LENGTH_BYTES)
            SecureRandom().nextBytes(salt)
            val hash = hashPassword(password.toCharArray(), salt, DEFAULT_ITERATIONS)
            val insertStmt =
                connection.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)
            insertStmt.setString(1, username)
            insertStmt.setBytes(2, hash)
            insertStmt.setBytes(3, salt)
            insertStmt.setInt(4, DEFAULT_ITERATIONS)
            return@withContext insertStmt.executeUpdate() == 1
        }

    private fun hashPassword(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
