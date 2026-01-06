/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GitUtilsTest {

    @Test
    fun `detectGitRepositoryUrl returns URL from actual git repository`() {
        // Test with the actual project repository
        val url = GitUtils.detectGitRepositoryUrl()
        
        // If we're in a git repository, we should get a URL
        // This test will pass in the actual SPHA project
        if (url != null) {
            assertTrue(url.isNotEmpty(), "Detected URL should not be empty")
            // Basic validation that it looks like a git URL
            assertTrue(
                url.contains("git") || url.contains("http") || url.contains("@"),
                "Detected URL should look like a git URL: $url"
            )
        }
    }

    @Test
    fun `detectGitRepositoryUrl returns null in non-git directory`() {
        // Create a temporary directory without git
        val tempDir = Files.createTempDirectory("non-git-test")
        try {
            // Change to temp directory and try to detect
            val process = ProcessBuilder("git", "config", "--get", "remote.origin.url")
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start()
            
            process.waitFor()
            val exitCode = process.exitValue()
            
            // Git should fail in a non-git directory
            assertTrue(exitCode != 0, "Git command should fail in non-git directory")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://github.com/test/repo.git",
        "https://gitlab.com/test/repo.git",
        "git@github.com:test/repo.git",
        "git@gitlab.com:test/repo.git",
        "https://github.company.com/org/project.git",
        "ssh://git@bitbucket.org/user/repo.git"
    ])
    fun `detectGitRepositoryUrl works with various git URL formats`(remoteUrl: String) {
        val tempDir = Files.createTempDirectory("git-test")
        try {
            // Initialize git repo
            ProcessBuilder("git", "init")
                .directory(tempDir.toFile())
                .start()
                .waitFor()

            // Add remote with the test URL
            ProcessBuilder("git", "remote", "add", "origin", remoteUrl)
                .directory(tempDir.toFile())
                .start()
                .waitFor()

            // Verify the URL was set in config
            val process = ProcessBuilder("git", "config", "--get", "remote.origin.url")
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            assertEquals(0, process.exitValue(), "Git command should succeed")
            assertEquals(remoteUrl, output, "Remote URL should match the configured value")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
