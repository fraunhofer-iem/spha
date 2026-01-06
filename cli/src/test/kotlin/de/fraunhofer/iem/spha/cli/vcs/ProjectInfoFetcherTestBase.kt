/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Base test class for ProjectInfoFetcher implementations.
 * Concrete test classes should extend this and implement the abstract methods.
 */
abstract class ProjectInfoFetcherTestBase {

    /** Creates the fetcher instance to test */
    protected abstract fun createFetcher(): ProjectInfoFetcher

    /** Returns the token for authentication, or null if not available */
    protected abstract fun getAuthToken(): String?

    /** Returns the repository URL to test against */
    protected abstract fun getTestRepositoryUrl(): String

    /** Returns the expected repository name */
    protected abstract fun getExpectedRepositoryName(): String

    /** Returns alternative URL formats for the test repository */
    protected abstract fun getAlternativeUrlFormats(): List<String>

    /** Returns a URL for a non-existent repository */
    protected abstract fun getNonExistentRepositoryUrl(): String

    /** Whether to skip tests if token is not available (default: true) */
    protected open val skipTestsIfNoToken: Boolean = true

    /** Whether to assert that stars >= 0 (default: true) */
    protected open val assertStarsNonNegative: Boolean = true

    /** Whether to assert that contributors >= 0 (default: true) */
    protected open val assertContributorsNonNegative: Boolean = true

    /** Whether to assert that commits >= 0 (default: true) */
    protected open val assertCommitsNonNegative: Boolean = true

    /** Whether to assert that last commit date is not null (default: true) */
    protected open val assertLastCommitDateNotNull: Boolean = true

    /** Whether to assert that languages list is not empty (default: true) */
    protected open val assertLanguagesNotEmpty: Boolean = true

    /** Whether the fetcher requires authentication (default: true) */
    protected open val requiresAuthentication: Boolean = true

    /** Whether the fetcher validates URL format (default: true, false for path-based fetchers) */
    protected open val validatesUrlFormat: Boolean = true

    @Test
    fun `getProjectInfo returns success for test repository`() = runBlocking {
        val token = getAuthToken()
        if (skipTestsIfNoToken && requiresAuthentication)
            assumeTrue(token != null, "Authentication token not available - skipping test")

        val fetcher = createFetcher()
        fetcher.use {
            val urls = listOf(getTestRepositoryUrl()) + getAlternativeUrlFormats()

            urls.forEach { url ->
                val result = it.getProjectInfo(url, token)

                assertTrue(result is NetworkResponse.Success, "Expected successful response for URL: $url")
                val projectInfo = result.data

                assertEquals(getExpectedRepositoryName(), projectInfo.name)

                if (assertStarsNonNegative) {
                    assertTrue(projectInfo.stars >= 0, "Stars must be >= 0, got ${projectInfo.stars}")
                }

                if (assertContributorsNonNegative) {
                    assertTrue(projectInfo.numberOfContributors >= 0, "Contributors must be >= 0, got ${projectInfo.numberOfContributors}")
                }

                if (assertCommitsNonNegative) {
                    assertNotNull(projectInfo.numberOfCommits, "Number of commits should not be null")
                    assertTrue(projectInfo.numberOfCommits >= 0, "Commits must be >= 0, got ${projectInfo.numberOfCommits}")
                }

                if (assertLastCommitDateNotNull) {
                    assertNotNull(projectInfo.lastCommitDate, "Last commit date should not be null")
                }

                if (assertLanguagesNotEmpty) {
                    assertTrue(projectInfo.usedLanguages.isNotEmpty(), "Languages should not be empty")
                }
            }
        }
    }

    @Test
    fun `getProjectInfo returns failure when token is not provided`() = runBlocking {
        if (!requiresAuthentication) {
            return@runBlocking
        }

        val fetcher = createFetcher()
        fetcher.use {
            val result = it.getProjectInfo(getTestRepositoryUrl(), tokenOverride = null)

            val hasEnvToken = getAuthToken() != null
            if (!hasEnvToken) {
                assertTrue(result is NetworkResponse.Failed, "Expected failure when no token provided")
                assertTrue(result.msg.contains("token", ignoreCase = true))
            }
        }
    }

    @Test
    fun `getProjectInfo returns failure for invalid token`() = runBlocking {
        if (!requiresAuthentication) {
            return@runBlocking
        }

        val fetcher = createFetcher()
        fetcher.use {
            val result = it.getProjectInfo(getTestRepositoryUrl(), tokenOverride = "abc123")

            assertTrue(result is NetworkResponse.Failed, "Expected failure for invalid token")
        }
    }

    @Test
    fun `getProjectInfo returns failure for invalid repository URL`() = runBlocking {
        val token = getAuthToken()
        if (skipTestsIfNoToken && requiresAuthentication) {
            assumeTrue(token != null, "Authentication token not available - skipping test")
        }

        val fetcher = createFetcher()
        fetcher.use {
            val result = it.getProjectInfo(getNonExistentRepositoryUrl(), token)

            assertTrue(result is NetworkResponse.Failed, "Expected failure for non-existent repository")
        }
    }

    @Test
    fun `getProjectInfo returns failure for malformed URL`() = runBlocking {
        if (!validatesUrlFormat) {
            return@runBlocking
        }

        val token = getAuthToken()
        if (skipTestsIfNoToken && requiresAuthentication) {
            assumeTrue(token != null, "Authentication token not available - skipping test")
        }

        val fetcher = createFetcher()
        fetcher.use {
            val result = it.getProjectInfo("not-a-valid-url", token)

            assertTrue(result is NetworkResponse.Failed, "Expected failure for malformed URL")
            val msg = result.msg
            assertTrue(msg.contains("Invalid", ignoreCase = true) ||
                      msg.contains("parse", ignoreCase = true) ||
                      msg.contains("error", ignoreCase = true))
        }
    }
}
