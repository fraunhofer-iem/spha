/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.utilities

import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FileSystemUtilitiesTest {

    // --- Relative Paths (cross-platform) ---

    @Test
    fun `resolves relative path`() {
        val result = FileSystemUtilities.resolvePathFromUriOrPath("relative/path/file.txt")
        assertNotNull(result)
        assertTrue(result.isAbsolute)
    }

    @Test
    fun `resolves current directory`() {
        val result = FileSystemUtilities.resolvePathFromUriOrPath(".")
        assertNotNull(result)
        assertEquals(Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize(), result)
    }

    @Test
    fun `resolves parent directory`() {
        val result = FileSystemUtilities.resolvePathFromUriOrPath("..")
        assertNotNull(result)
        val userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        assertEquals(userDir.parent, result)
    }

    // --- Absolute Paths (Windows) ---

    @ParameterizedTest
    @ValueSource(strings = ["C:\\Users\\test\\project", "C:/Users/test/project"])
    @EnabledOnOs(OS.WINDOWS)
    fun `resolves Windows absolute path`(path: String) {
        val result = FileSystemUtilities.resolvePathFromUriOrPath(path)
        assertNotNull(result)
        assertTrue(result.isAbsolute)
        assertTrue(result.toString().startsWith("C:"))
    }

    // --- Absolute Paths (Unix) ---

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `resolves Unix absolute path`() {
        val result = FileSystemUtilities.resolvePathFromUriOrPath("/home/user/project")
        assertNotNull(result)
        assertTrue(result.isAbsolute)
        assertTrue(result.toString().startsWith("/"))
    }

    // --- File URI Scheme ---

    @Test
    fun `resolves file URI to correct path`() {
        val tempDir = createTempDirectory("test-file-uri")
        try {
            val fileUri = tempDir.toUri().toString()
            assertTrue(fileUri.startsWith("file:"))

            val result = FileSystemUtilities.resolvePathFromUriOrPath(fileUri)
            assertNotNull(result)
            assertEquals(tempDir.toAbsolutePath().normalize(), result)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `callback is not invoked for file scheme`() {
        val tempDir = createTempDirectory("callback-test")
        try {
            var callbackCalled = false
            val fileUri = tempDir.toUri().toString()

            FileSystemUtilities.resolvePathFromUriOrPath(fileUri) {
                callbackCalled = true
                null
            }

            assertFalse(callbackCalled, "Callback should not be called for file: scheme")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // --- HTTP Scheme (callback behavior) ---

    @Test
    fun `invokes callback for http scheme`() {
        var callbackInvoked = false

        FileSystemUtilities.resolvePathFromUriOrPath("http://example.com/path") {
            callbackInvoked = true
            null
        }

        assertTrue(callbackInvoked, "Callback should be invoked for http: scheme")
    }

    @Test
    fun `invokes callback for https scheme`() {
        var callbackInvoked = false

        FileSystemUtilities.resolvePathFromUriOrPath("https://github.com/owner/repo") {
            callbackInvoked = true
            null
        }

        assertTrue(callbackInvoked, "Callback should be invoked for https: scheme")
    }

    @Test
    fun `returns callback result for http scheme`() {
        val fallbackPath = Path.of("/fallback/path")

        val result =
            FileSystemUtilities.resolvePathFromUriOrPath("https://github.com/test/repo") {
                fallbackPath
            }

        assertNotNull(result)
        assertEquals(fallbackPath.toAbsolutePath().normalize(), result)
    }

    @Test
    fun `returns null when callback returns null`() {
        val result =
            FileSystemUtilities.resolvePathFromUriOrPath("https://example.com/path") { null }
        assertNull(result)
    }

    @Test
    fun `default callback returns user directory for http scheme`() {
        val result = FileSystemUtilities.resolvePathFromUriOrPath("https://example.com/path")
        assertNotNull(result)
        assertEquals(Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize(), result)
    }
}
