/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.agent.spi.expression.spel

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpelLogicalExpressionParserTest {

    private val parser = SpelLogicalExpressionParser()

    @Test
    fun `parse returns null for non-spel expression`() {
        val result = parser.parse("it:UserInput")
        assertNull(result, "Parser should return null for expressions without 'spel:' prefix")
    }

    @Test
    fun `parse returns null for empty string`() {
        val result = parser.parse("")
        assertNull(result, "Parser should return null for empty string")
    }

    @Test
    fun `parse returns SpelLogicalExpression for valid spel expression`() {
        val result = parser.parse("spel:elephant.age > 20")
        assertNotNull(result, "Parser should return a SpelLogicalExpression for valid expression")
        assertTrue(result is SpelLogicalExpression, "Result should be a SpelLogicalExpression")
    }

    @Test
    fun `parse extracts expression correctly`() {
        val result = parser.parse("spel:user.name == 'John'")
        assertNotNull(result, "Parser should return a SpelLogicalExpression")
    }

    @Test
    fun `parse handles complex expressions`() {
        val result = parser.parse("spel:person.age >= 18 && person.country == 'USA'")
        assertNotNull(result, "Parser should handle complex boolean expressions")
    }

    @Test
    fun `parse with only prefix returns empty expression`() {
        val result = parser.parse("spel:")
        assertNotNull(result, "Parser should return an expression even for 'spel:' alone")
    }

    @Test
    fun `parse case sensitive prefix`() {
        val result = parser.parse("SPEL:test")
        assertNull(result, "Parser should be case-sensitive for prefix")
    }

    @Test
    fun `parse preserves whitespace in expression`() {
        val result = parser.parse("spel:  elephant.age > 20  ")
        assertNotNull(result, "Parser should preserve whitespace in expression")
    }
}
