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

        if (url != null) {
            assertTrue(url.isNotEmpty(), "Detected URL should not be empty")
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
            val url = GitUtils.detectGitRepositoryUrl(tempDir.toFile())
            assertNull(url, "detectGitRepositoryUrl should return null in non-git directory")
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
            // Initialize git repo using GitUtils
            GitUtils.runGitCommand(tempDir.toFile(), "init")
            GitUtils.runGitCommand(tempDir.toFile(), "remote", "add", "origin", remoteUrl)

            val detectedUrl = GitUtils.detectGitRepositoryUrl(tempDir.toFile())
            assertNotNull(detectedUrl, "detectGitRepositoryUrl should return a URL")
            assertEquals(remoteUrl, detectedUrl, "Detected URL should match the configured remote URL")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
