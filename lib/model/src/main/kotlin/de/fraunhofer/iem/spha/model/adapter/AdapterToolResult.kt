/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

import kotlinx.serialization.Serializable

/**
 * Represents a sealed interface serving as a marker for various origin-related data types. It is
 * designed to model and group different implementations that signify the origin of specific data or
 * results within a system or a tool's output.
 *
 * Implementations of this interface may vary based on their usage. For instance, they could
 * represent the origin of analysis results or details from different tools.
 */
@Serializable sealed interface Origin
