/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.cli.utilities

import java.net.URI
import java.nio.file.Path

internal object FileSystemUtilities {

    fun resolvePathFromUriOrPath(
        path: String,
        onUnknownScheme: () -> Path? = { Path.of(System.getProperty("user.dir")) },
    ): Path? {
        return try {
                val uri = URI.create(path)
                when {
                    uri.scheme == "file" -> Path.of(uri)
                    uri.scheme == null -> Path.of(path)
                    // Windows drive letter (single char scheme like C:, D:, etc.)
                    uri.scheme.length == 1 && uri.scheme[0].isLetter() -> Path.of(path)
                    else -> onUnknownScheme()
                }
            } catch (_: Exception) {
                Path.of(path)
            }
            ?.toAbsolutePath()
            ?.normalize()
    }
}
