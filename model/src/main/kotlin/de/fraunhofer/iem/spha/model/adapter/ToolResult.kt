/*
 * Copyright (c) 2025 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.model.adapter

/**
 * Represents a base interface for results produced by various tools or analyzers.
 *
 * Classes implementing this interface encapsulate the output and data generated
 * by specific tools. It is used as a common contract that different tool result
 * types can adhere to, allowing polymorphic handling of these results.
 *
 * Implementations of this interface typically include structured data
 * generated from specific tools, such as analysis outcomes, configurations,
 * or metadata, serialized for interoperability and further processing.
 */
sealed interface ToolResult
