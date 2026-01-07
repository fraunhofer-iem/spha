/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

class GitHubProjectFetcherTest : ProjectInfoFetcherTestBase() {

    override fun createFetcher(): ProjectInfoFetcher = GitHubProjectFetcher()

    override fun getAuthToken(): String? =
        System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")

    override fun getTestRepositoryUrl(): String = "https://github.com/fraunhofer-iem/spha"

    override fun getExpectedRepositoryName(): String = "spha"

    override fun getAlternativeUrlFormats(): List<String> =
        listOf(
            "https://github.com/fraunhofer-iem/spha",
            "https://github.com/fraunhofer-iem/spha.git",
            "git@github.com:fraunhofer-iem/spha.git",
            "git@github.com:fraunhofer-iem/spha",
        )

    override fun getNonExistentRepositoryUrl(): String =
        "https://github.com/fraunhofer-iem/nonexistent-repo-67890"

    override fun getWrongOriginUrls(): List<String> =
        listOf(
            "file:///tmp/some/local/path",
            "/tmp/some/local/path"
        )
}
