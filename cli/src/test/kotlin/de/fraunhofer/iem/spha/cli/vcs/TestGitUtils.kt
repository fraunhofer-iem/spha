/*
 * Copyright (c) 2025-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.vcs

/** Utility functions for Git operations in tests. */
object TestGitUtils {
    /**
     * Normalizes a Git URL to a standard HTTPS format. Handles SSH URLs, URLs with/without .git,
     * etc.
     *
     * @param url The Git URL to normalize
     * @return The normalized HTTPS URL, or the original if it cannot be normalized
     */
    fun normalizeGitUrl(url: String): String {
        // Handle common git hosting providers: github.com, gitlab.com, bitbucket.org
        val regex = Regex("""(?:https?://|ssh://|git@)(?:git@)?([^/:]+)[:/]([^/]+)/([^/.]+)(?:\.git)?""")
        val matchResult = regex.find(url) ?: return url

        val (host, owner, repo) = matchResult.destructured
        return "https://$host/$owner/$repo"
    }
}
