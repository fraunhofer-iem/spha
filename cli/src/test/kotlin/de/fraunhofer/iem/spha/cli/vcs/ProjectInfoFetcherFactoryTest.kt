/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProjectInfoFetcherFactoryTest {

    companion object {
        @JvmStatic
        fun stringTypeCases() = Stream.of(
            Arguments.of("github", RepositoryType.GITHUB),
            Arguments.of("GITLAB", RepositoryType.GITLAB),
            Arguments.of("local", RepositoryType.LOCAL)
        )

        @JvmStatic
        fun detectRepositoryTypeCases() = Stream.of(
            Arguments.of("https://github.com/owner/repo", RepositoryType.GITHUB),
            Arguments.of("git@github.com:owner/repo.git", RepositoryType.GITHUB),
            Arguments.of("https://gitlab.com/owner/repo", RepositoryType.GITLAB),
            Arguments.of("https://gitlab.company.com/owner/repo", RepositoryType.GITLAB),
            Arguments.of("/some/random/path", RepositoryType.LOCAL)
        )

        @JvmStatic
        fun createFetcherFromUrlCases() = Stream.of(
            Arguments.of("https://github.com/owner/repo", GitHubProjectFetcher::class),
            Arguments.of("https://gitlab.com/owner/repo", GitLabProjectFetcher::class)
        )

        @JvmStatic
        fun caseInsensitiveCases() = Stream.of(
            Arguments.of("github", RepositoryType.GITHUB),
            Arguments.of("GITHUB", RepositoryType.GITHUB),
            Arguments.of("GitHub", RepositoryType.GITHUB),
            Arguments.of("gitlab", RepositoryType.GITLAB),
            Arguments.of("GITLAB", RepositoryType.GITLAB),
            Arguments.of("local", RepositoryType.LOCAL),
            Arguments.of("LOCAL", RepositoryType.LOCAL)
        )
    }

    @ParameterizedTest
    @EnumSource(RepositoryType::class)
    fun `createFetcher with RepositoryType creates correct fetcher`(type: RepositoryType) {
        val fetcher = ProjectInfoFetcherFactory.createFetcher(type)
        fetcher.use {
            when (type) {
                RepositoryType.GITHUB -> assertTrue(it is GitHubProjectFetcher)
                RepositoryType.GITLAB -> assertTrue(it is GitLabProjectFetcher)
                RepositoryType.LOCAL -> assertTrue(it is LocalRepositoryFetcher)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("stringTypeCases")
    fun `createFetcher with string creates correct fetcher`(typeString: String, expectedType: RepositoryType) {
        val fetcher = ProjectInfoFetcherFactory.createFetcher(typeString)
        fetcher.use {
            when (expectedType) {
                RepositoryType.GITHUB -> assertTrue(it is GitHubProjectFetcher)
                RepositoryType.GITLAB -> assertTrue(it is GitLabProjectFetcher)
                RepositoryType.LOCAL -> assertTrue(it is LocalRepositoryFetcher)
            }
        }
    }

    @Test
    fun `createFetcher with invalid string throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ProjectInfoFetcherFactory.createFetcher("invalid")
        }
        assertEquals(exception.message?.contains("Invalid repository type"), true)
    }

    @ParameterizedTest
    @MethodSource("detectRepositoryTypeCases")
    fun `detectRepositoryType detects correct type from URL`(url: String, expectedType: RepositoryType) {
        val type = ProjectInfoFetcherFactory.detectRepositoryType(url)
        assertEquals(expectedType, type)
    }

    @Test
    fun `detectRepositoryType throws for unknown host`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ProjectInfoFetcherFactory.detectRepositoryType("https://unknown-host.com/owner/repo")
        }
        assertEquals(exception.message?.contains("Unable to determine repository type"), true)
        assertEquals(exception.message?.contains("unknown-host.com"), true)
        assertEquals(exception.message?.contains("--repositoryType"), true)
    }

    @Test
    fun `detectRepositoryType detects local repository from existing path`() {
        val tempDir = Files.createTempDirectory("git-test")
        try {
            GitUtils.runGitCommand(tempDir.toFile(), "init")
            val type = ProjectInfoFetcherFactory.detectRepositoryType(tempDir.toString())
            assertEquals(RepositoryType.LOCAL, type)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @ParameterizedTest
    @MethodSource("createFetcherFromUrlCases")
    fun `createFetcherFromUrl creates correct fetcher for URL`(url: String, expectedClass: kotlin.reflect.KClass<*>) {
        val fetcher = ProjectInfoFetcherFactory.createFetcherFromUrl(url)
        fetcher.use {
            assertTrue(expectedClass.isInstance(it))
        }
    }

    @Test
    fun `createFetcherFromUrl creates LocalRepositoryFetcher for local path`() {
        val tempDir = Files.createTempDirectory("git-test")
        try {
            GitUtils.runGitCommand(tempDir.toFile(), "init")
            val fetcher = ProjectInfoFetcherFactory.createFetcherFromUrl(tempDir.toString())
            fetcher.use {
                assertTrue(it is LocalRepositoryFetcher)
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `createFetcherFromUrl throws for unknown host`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ProjectInfoFetcherFactory.createFetcherFromUrl("https://unknown-host.com/owner/repo")
        }
        assertEquals(exception.message?.contains("Unable to determine repository type"), true)
        assertEquals(exception.message?.contains("--repositoryType"), true)
    }

    @ParameterizedTest
    @MethodSource("caseInsensitiveCases")
    fun `RepositoryType fromString is case insensitive`(input: String, expected: RepositoryType) {
        assertEquals(expected, RepositoryType.fromString(input))
    }
}
