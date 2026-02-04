/*
 * Copyright (c) 2024-2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.processors

import de.fraunhofer.iem.spha.adapter.AdapterResult
import de.fraunhofer.iem.spha.adapter.ToolProcessor
import de.fraunhofer.iem.spha.adapter.TransformationResult
import de.fraunhofer.iem.spha.model.adapter.Origin
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.fail

/**
 * Abstract base test class for processor tests. Provides common test patterns for testing tool
 * processors.
 *
 * Subclasses should implement the abstract methods to provide processor-specific test data and
 * validation logic.
 */
abstract class AbstractProcessorTest<T : Origin> {

    /**
     * Returns a list of invalid JSON strings that should cause the processor to return null. These
     * are strings that don't match the processor's expected format.
     */
    abstract val invalidInputs: List<String>

    /**
     * Returns a list of valid test resource file paths (relative to src/test/resources). Each file
     * should contain valid input for the processor.
     */
    abstract val validTestResourceFiles: List<String>

    /**
     * Returns whether the processor supports empty input (returns a valid result for empty/blank
     * content). Default is false. Override to return true if the processor handles empty input
     * specially.
     */
    open val supportsEmptyInput: Boolean
        get() = false

    /**
     * Returns the processor instance to be tested. Subclasses must implement this to provide the
     * specific processor.
     */
    internal abstract fun getProcessor(): ToolProcessor

    /**
     * Processes the given content string and returns the result. This is a non-virtual method that
     * delegates to the processor returned by [getProcessor].
     *
     * @param content The JSON content to process.
     * @return The AdapterResult if processing succeeds, or null if the content doesn't match the
     *   processor's format.
     */
    fun process(content: String): AdapterResult<T>? {
        return getProcessor().tryProcess(content) as AdapterResult<T>?
    }

    /**
     * Validates the result from processing a valid input. Subclasses can override this to add
     * processor-specific validation.
     *
     * @param result The AdapterResult to validate.
     * @param resourceFile The resource file that was processed (for context in assertions).
     */
    open fun validateResult(result: AdapterResult<T>, resourceFile: String) {
        // Default implementation is empty - base class already validates Success<T> in
        // testValidResourceFiles
    }

    /**
     * Returns whether all transformation results must be Success<T> for the given resource file.
     * Default is true. Override to return false for specific files if the processor may produce
     * some Error results (e.g., when some data entries are invalid but others are valid).
     *
     * @param resourceFile The resource file being tested.
     */
    open fun expectAllResultsSuccess(resourceFile: String): Boolean = true

    /**
     * Validates the result from processing empty input. Only called if [getSupportsEmptyInput]
     * returns true.
     *
     * @param result The AdapterResult from processing empty input.
     */
    open fun validateEmptyInputResult(result: AdapterResult<T>) {
        // Default implementation - subclasses can override for specific validation
        if (supportsEmptyInput)
            fail {
                "Override this method to validate empty input results for processors that support empty input."
            }
    }

    @Test
    fun testInvalidInputReturnsNull() {
        invalidInputs.forEach { invalidInput ->
            assertNull(process(invalidInput), "Expected null for invalid input: $invalidInput")
        }
    }

    @Test
    fun testEmptyInput() {
        val result = assertDoesNotThrow { process("") }

        if (supportsEmptyInput) {
            assertNotNull(result, "Processor supports empty input but returned null")
            // Assert all results are Success<T>
            result.transformationResults.forEach {
                assertTrue(
                    it is TransformationResult.Success<T>,
                    "Expected all results to be Success<T>",
                )
            }
            validateEmptyInputResult(result)
        } else {
            assertNull(result, "Expected null for empty input")
        }
    }

    @Test
    fun testValidResourceFiles() {
        val resourceFiles = validTestResourceFiles
        if (resourceFiles.isEmpty()) {
            fail { "No valid test resource files found." }
        }

        resourceFiles.forEach { resourceFile ->
            val content = File("src/test/resources/$resourceFile").readText()

            val result = assertDoesNotThrow { process(content) }

            assertNotNull(result, "Expected non-null result for $resourceFile")
            // Assert results contain Success<T>
            val successes =
                result.transformationResults.filterIsInstance<TransformationResult.Success<T>>()
            assertTrue(
                successes.isNotEmpty(),
                "Expected at least one Success<T> result for $resourceFile",
            )
            if (expectAllResultsSuccess(resourceFile)) {
                assertEquals(
                    successes.size,
                    result.transformationResults.size,
                    "Expected all results to be Success<T> for $resourceFile, but got ${result.transformationResults.size - successes.size} non-success results",
                )
            }
            validateResult(result, resourceFile)
        }
    }
}
