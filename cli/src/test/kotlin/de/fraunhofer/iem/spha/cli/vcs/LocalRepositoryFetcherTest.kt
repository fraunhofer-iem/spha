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
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LocalRepositoryFetcherTest : ProjectInfoFetcherTestBase() {

    private fun createTestGitRepo(name: String = "test-repo"): Pair<String, () -> Unit> {
        val tempDir = Files.createTempDirectory("git-test-$name")
        val dir = tempDir

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
        return getTestRepositoryUrl()
    }

    override fun getAlternativeUrlFormats(): List<String> {
        val repoPath = Path(getTestRepositoryUrl())
        val uri = repoPath.toUri().toString()
        val formats = mutableListOf(uri)

        // Add a variant with trailing slash if it doesn't have one
        if (!uri.endsWith("/")) {
            formats.add("$uri/")
        }

        val relativePath = Path(System.getProperty("user.dir")).relativize(repoPath)

        formats.add(relativePath.toString())
        if (System.getProperty("os.name").startsWith("Windows")) {
            formats.add(repoPath.toString().replace('\\', '/'))
        }

        return formats
    }

    override fun getNonExistentRepositoryUrl(): String = "/non/existent/path"

    override fun getWrongOriginUrls(): List<String> =
        listOf("https://github.com/fraunhofer-iem/spha")

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

    // Local-specific tests (not covered by base class)

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
            val dir = tempDir
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
            val dir = Path(repoPath)
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

    @Test
    fun `getProjectInfo returns failure for file URI pointing to a file`() = runTest {
        val tempFile = kotlin.io.path.createTempFile("test-file", ".txt")
        try {
            val fetcher = LocalRepositoryFetcher()
            val fileUri = tempFile.toUri().toString()

            val result = fetcher.getProjectInfo(fileUri, null)

            assertTrue(
                result is NetworkResponse.Failed,
                "Expected failure for file URI pointing to a file",
            )
            assertTrue(
                result.msg.contains("not a directory"),
                "Error message should mention 'not a directory'",
            )
        } finally {
            tempFile.deleteIfExists()
        }
    }

    // Local-specific commitSha tests (testing specific commit behavior)

    @Test
    fun `getProjectInfo with specific commitSha returns data for that commit`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val dir = Path(repoPath)

            // Get the SHA of the first commit
            val firstCommitSha = GitUtils.runGitCommand(dir, "rev-parse", "HEAD")!!

            // Add a second commit
            dir.resolve("file2.kt").writeText("// Second file")
            GitUtils.runGitCommand(dir, "add", ".")
            GitUtils.runGitCommand(dir, "commit", "-m", "Second commit")

            // Verify we now have 2 commits at HEAD
            val resultAtHead = LocalRepositoryFetcher().getProjectInfo(repoPath)
            assertTrue(resultAtHead is NetworkResponse.Success)
            assertEquals(2, resultAtHead.data.numberOfCommits, "Should have 2 commits at HEAD")

            // Query with the first commit SHA - should only count 1 commit
            val resultAtFirstCommit =
                LocalRepositoryFetcher().getProjectInfo(repoPath, null, firstCommitSha)
            assertTrue(resultAtFirstCommit is NetworkResponse.Success)
            assertEquals(
                1,
                resultAtFirstCommit.data.numberOfCommits,
                "Should have 1 commit at first commit SHA",
            )
        } finally {
            cleanup()
        }
    }

    @Test
    fun `getProjectInfo with commitSha returns correct last commit date`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val dir = Path(repoPath)

            // Get the date of the first commit
            val firstCommitSha = GitUtils.runGitCommand(dir, "rev-parse", "HEAD")!!
            val firstCommitDate =
                GitUtils.runGitCommand(dir, "log", "-1", "--format=%cI", firstCommitSha)

            // Wait a moment and add a second commit
            Thread.sleep(1000)
            dir.resolve("file2.kt").writeText("// Second file")
            GitUtils.runGitCommand(dir, "add", ".")
            GitUtils.runGitCommand(dir, "commit", "-m", "Second commit")

            // Query with the first commit SHA
            val result = LocalRepositoryFetcher().getProjectInfo(repoPath, null, firstCommitSha)

            assertTrue(result is NetworkResponse.Success)
            assertEquals(
                firstCommitDate,
                result.data.lastCommitDate,
                "Last commit date should match the first commit's date",
            )
        } finally {
            cleanup()
        }
    }

    @Test
    fun `getProjectInfo with commitSha returns correct contributor count`() = runTest {
        val (repoPath, cleanup) = createTestGitRepo()
        try {
            val dir = Path(repoPath)

            // Get the SHA of the first commit (made by "Test User")
            val firstCommitSha = GitUtils.runGitCommand(dir, "rev-parse", "HEAD")!!

            // Add a second commit with a different author
            GitUtils.runGitCommand(dir, "config", "user.email", "another@example.com")
            GitUtils.runGitCommand(dir, "config", "user.name", "Another User")
            dir.resolve("file2.kt").writeText("// Second file")
            GitUtils.runGitCommand(dir, "add", ".")
            GitUtils.runGitCommand(dir, "commit", "-m", "Second commit")

            // At HEAD, should have 2 contributors
            val resultAtHead = LocalRepositoryFetcher().getProjectInfo(repoPath)
            assertTrue(resultAtHead is NetworkResponse.Success)
            assertEquals(
                2,
                resultAtHead.data.numberOfContributors,
                "Should have 2 contributors at HEAD",
            )

            // At first commit, should have 1 contributor
            val resultAtFirstCommit =
                LocalRepositoryFetcher().getProjectInfo(repoPath, null, firstCommitSha)
            assertTrue(resultAtFirstCommit is NetworkResponse.Success)
            assertEquals(
                1,
                resultAtFirstCommit.data.numberOfContributors,
                "Should have 1 contributor at first commit",
            )
        } finally {
            cleanup()
        }
    }
}
