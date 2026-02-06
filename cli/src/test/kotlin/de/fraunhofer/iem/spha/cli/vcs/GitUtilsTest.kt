/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import java.nio.file.Files
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
                "Detected URL should look like a git URL: $url",
            )
        }
    }

    @Test
    fun `detectGitRepositoryUrl returns null in non-git directory`() {
        // Create a temporary directory without git
        val tempDir = Files.createTempDirectory("non-git-test")
        try {
            val url = GitUtils.detectGitRepositoryUrl(tempDir)
            assertNull(url, "detectGitRepositoryUrl should return null in non-git directory")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "https://github.com/test/repo.git",
                "https://gitlab.com/test/repo.git",
                "git@github.com:test/repo.git",
                "git@gitlab.com:test/repo.git",
                "https://github.company.com/org/project.git",
                "ssh://git@bitbucket.org/user/repo.git",
            ]
    )
    fun `detectGitRepositoryUrl works with various git URL formats`(remoteUrl: String) {
        val tempDir = Files.createTempDirectory("git-test")
        try {
            // Initialize git repo using GitUtils
            GitUtils.runGitCommand(tempDir, "init")
            GitUtils.runGitCommand(tempDir, "remote", "add", "origin", remoteUrl)

            val detectedUrl = GitUtils.detectGitRepositoryUrl(tempDir)
            assertNotNull(detectedUrl, "detectGitRepositoryUrl should return a URL")
            assertEquals(
                remoteUrl,
                detectedUrl,
                "Detected URL should match the configured remote URL",
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(
        value =
            [
                "https://github.com/fraunhofer-iem/spha,https://github.com/fraunhofer-iem/spha",
                "https://github.com/fraunhofer-iem/spha.git,https://github.com/fraunhofer-iem/spha",
                "git@github.com:fraunhofer-iem/spha.git,https://github.com/fraunhofer-iem/spha",
                "git@github.com:fraunhofer-iem/spha,https://github.com/fraunhofer-iem/spha",
                "ssh://git@github.com/fraunhofer-iem/spha.git,https://github.com/fraunhofer-iem/spha",
                "https://gitlab.com/group/project.git,https://gitlab.com/group/project",
                "git@gitlab.com:group/project.git,https://gitlab.com/group/project",
                "git@bitbucket.org:user/repo.git,https://bitbucket.org/user/repo",
                "https://www.github.com/fraunhofer-iem/spha,https://github.com/fraunhofer-iem/spha",
                "https://www.gitlab.com/group/project,https://gitlab.com/group/project",
            ]
    )
    fun `normalizeGitUrl correctly normalizes various formats`(input: String, expected: String) {
        assertEquals(expected, TestGitUtils.normalizeGitUrl(input))
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(
        value =
            [
                "https://github.com/owner/repo,github.com,owner/repo",
                "https://www.github.com/owner/repo,github.com,owner/repo",
                "http://github.com/owner/repo.git,github.com,owner/repo",
                "git@github.com:owner/repo.git,github.com,owner/repo",
                "https://gitlab.com/group/subgroup/project.git,gitlab.com,group/subgroup/project",
                "https://gitlab.my-company.com/project,gitlab.my-company.com,project",
                "git@gitlab.my-company.com:project.git,gitlab.my-company.com,project",
            ]
    )
    fun `parseVCSUrl correctly parses various formats`(
        input: String,
        expectedHost: String,
        expectedPath: String,
    ) {
        val result = GitUtils.parseVCSUrl(input)
        assertNotNull(result)
        assertEquals(expectedHost, result.first)
        assertEquals(expectedPath, result.second)
    }

    @Test
    fun `getCurrentCommitSha returns SHA from actual git repository`() {
        // Test with the actual project repository
        val sha = GitUtils.getCurrentCommitSha()

        assertNotNull(sha, "getCurrentCommitSha should return a SHA in a git repository")
        assertTrue(sha.isNotEmpty(), "SHA should not be empty")
        // Git SHA is 40 hex characters
        assertEquals(sha.length, 40, "SHA should be 40 characters, got ${sha.length}")
        assertTrue(sha.all { it in '0'..'9' || it in 'a'..'f' }, "SHA should be hexadecimal")
    }

    @Test
    fun `getCurrentCommitSha returns null in non-git directory`() {
        val tempDir = Files.createTempDirectory("non-git-test")
        try {
            val sha = GitUtils.getCurrentCommitSha(tempDir)
            assertNull(sha, "getCurrentCommitSha should return null in non-git directory")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `getCurrentCommitSha returns correct SHA for git repository`() {
        val tempDir = Files.createTempDirectory("git-test")
        try {
            // Initialize git repo
            GitUtils.runGitCommand(tempDir, "init")
            GitUtils.runGitCommand(tempDir, "config", "user.email", "test@example.com")
            GitUtils.runGitCommand(tempDir, "config", "user.name", "Test User")

            // Create a file and commit
            tempDir.resolve("test.txt").toFile().writeText("test content")
            GitUtils.runGitCommand(tempDir, "add", ".")
            GitUtils.runGitCommand(tempDir, "commit", "-m", "Initial commit")

            val sha = GitUtils.getCurrentCommitSha(tempDir)

            assertNotNull(sha, "getCurrentCommitSha should return a SHA")
            assertEquals(40, sha.length, "SHA should be 40 characters")

            // Verify it matches what git rev-parse returns
            val expectedSha = GitUtils.runGitCommand(tempDir, "rev-parse", "HEAD")
            assertEquals(expectedSha, sha, "SHA should match git rev-parse HEAD")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
