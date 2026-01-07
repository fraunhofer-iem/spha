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
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlin.io.path.Path

class LocalRepositoryFetcherTest : ProjectInfoFetcherTestBase() {

    private fun createTestGitRepo(name: String = "test-repo"): Pair<String, () -> Unit> {
        val tempDir = Files.createTempDirectory("git-test-$name")
        val dir = tempDir.toFile()

        // Initialize and configure git repository
        GitUtils.runGitCommand(dir, "init")
        GitUtils.runGitCommand(dir, "config", "user.email", "test@example.com")
        GitUtils.runGitCommand(dir, "config", "user.name", "Test User")
        GitUtils.runGitCommand(dir, "remote", "add", "origin", "https://github.com/test/repo.git")

        // Create test files
        tempDir.resolve("README.md").writeText("# Test Repository")
        tempDir.resolve("test.kt").writeText("fun main() { println(\"Hello\") }")
        tempDir.resolve("script.py").writeText("print('Hello from Python')")

        // Commit files
        GitUtils.runGitCommand(dir, "add", ".")
        GitUtils.runGitCommand(dir, "commit", "-m", "Initial commit")

        val cleanup: () -> Unit = { tempDir.toFile().deleteRecursively() }
        return Pair(tempDir.toString(), cleanup)
    }

    // Base test implementation
    private var testRepoPath: String? = null
    private var testRepoCleanup: (() -> Unit)? = null

    override fun createFetcher(): ProjectInfoFetcher = LocalRepositoryFetcher()

    override fun getAuthToken(): String? = null // Local repos don't need auth

    override fun getTestRepositoryUrl(): String {
        if (testRepoPath == null) {
            val (path, cleanup) = createTestGitRepo()
            testRepoPath = path
            testRepoCleanup = cleanup
        }
        return testRepoPath!!
    }

    override fun getExpectedRepositoryName(): String {
        val path = getTestRepositoryUrl()
        return Path(path).toRealPath().toString()
    }

    override fun getAlternativeUrlFormats(): List<String> =
        emptyList() // No alternative formats for local paths

    override fun getNonExistentRepositoryUrl(): String = "/non/existent/path"

    override val requiresAuthentication: Boolean = false
    override val assertStarsNonNegative: Boolean = false // Local repos have stars = -1
    override val validatesUrlFormat: Boolean = false // Local fetcher validates paths, not URLs

    // Clean up after base tests
    @org.junit.jupiter.api.AfterEach
    fun cleanupBaseTest() {
        testRepoCleanup?.invoke()
        testRepoPath = null
        testRepoCleanup = null
    }

    // Special tests for LocalRepositoryFetcher

    @Test
    fun `getProjectInfo detects languages from file extensions`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val result = LocalRepositoryFetcher().getProjectInfo(repoPath)

            assertTrue(result is NetworkResponse.Success)
            val languageNames = result.data.usedLanguages.map { it.name }
            assertTrue(languageNames.contains("Kotlin"), "Should detect Kotlin files")
            assertTrue(languageNames.contains("Python"), "Should detect Python files")
        } finally {
            cleanup()
        }
    }

    @Test
    fun `getProjectInfo counts commits correctly`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val dir = java.io.File(repoPath)
            dir.resolve("file2.kt").writeText("// Another file")
            GitUtils.runGitCommand(dir, "add", ".")
            GitUtils.runGitCommand(dir, "commit", "-m", "Second commit")

            val result = LocalRepositoryFetcher().getProjectInfo(repoPath)

            assertTrue(result is NetworkResponse.Success)
            assertEquals(2, result.data.numberOfCommits, "Should count 2 commits")
        } finally {
            cleanup()
        }
    }

    @Test
    fun `getProjectInfo gets last commit date`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val result = LocalRepositoryFetcher().getProjectInfo(repoPath)

            assertTrue(result is NetworkResponse.Success)
            assertEquals(
                result.data.lastCommitDate?.isNotEmpty(),
                true,
                "Should have last commit date",
            )
        } finally {
            cleanup()
        }
    }

    @Test
    fun `getProjectInfo counts contributors`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val result = LocalRepositoryFetcher().getProjectInfo(repoPath)

            assertTrue(result is NetworkResponse.Success)
            assertTrue(result.data.numberOfContributors >= 1, "Should have at least 1 contributor")
        } finally {
            cleanup()
        }
    }

    @Test
    fun `getProjectInfo returns failed for non-existent directory`() = runTest {
        val result = LocalRepositoryFetcher().getProjectInfo("/non/existent/path")

        assertTrue(result is NetworkResponse.Failed, "Should fail for non-existent path")
        assertTrue(result.msg.contains("does not exist"), "Error should mention non-existent path")
    }

    @Test
    fun `getProjectInfo returns failed for non-git directory`() = runTest {
        val tempDir = Files.createTempDirectory("non-git-test")
        try {
            val result = LocalRepositoryFetcher().getProjectInfo(tempDir.toString())

            assertTrue(result is NetworkResponse.Failed, "Should fail for non-git directory")
            assertTrue(
                result.msg.contains("Not a git repository"),
                "Error should mention not a git repo",
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `getProjectInfo works with repository without remote`() = runTest {
        val tempDir = Files.createTempDirectory("git-no-remote")
        try {
            val dir = tempDir.toFile()
            GitUtils.runGitCommand(dir, "init")
            GitUtils.runGitCommand(dir, "config", "user.email", "test@example.com")
            GitUtils.runGitCommand(dir, "config", "user.name", "Test User")
            tempDir.resolve("test.txt").writeText("test")
            GitUtils.runGitCommand(dir, "add", ".")
            GitUtils.runGitCommand(dir, "commit", "-m", "Initial commit")

            val result = LocalRepositoryFetcher().getProjectInfo(tempDir.toString())

            assertTrue(result is NetworkResponse.Success, "Should succeed even without remote")
            assertEquals(
                tempDir.toString(),
                result.data.url,
                "URL should be the local path when no remote",
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `getProjectInfo detects multiple languages proportionally`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val dir = java.io.File(repoPath)
            dir.resolve("Main.java").writeText("public class Main { /* Large file */ }".repeat(100))
            dir.resolve("small.js").writeText("console.log('hi');")
            GitUtils.runGitCommand(dir, "add", ".")
            GitUtils.runGitCommand(dir, "commit", "-m", "Add more languages")

            val result = LocalRepositoryFetcher().getProjectInfo(repoPath)

            assertTrue(result is NetworkResponse.Success)
            val languages = result.data.usedLanguages.associateBy { it.name }
            assertTrue(languages.containsKey("Java"), "Should detect Java")
            assertTrue(languages.containsKey("JavaScript"), "Should detect JavaScript")

            val javaSize = languages["Java"]?.size ?: 0
            val jsSize = languages["JavaScript"]?.size ?: 0
            assertTrue(javaSize > jsSize, "Java file should be larger than JS file")
        } finally {
            cleanup()
        }
    }
}
