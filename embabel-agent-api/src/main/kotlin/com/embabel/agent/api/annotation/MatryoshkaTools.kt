/*
 * Copyright 2024-2026 Embabel Pty Ltd.
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
package com.embabel.agent.api.annotation

/**
 * Marks a class as an UnfoldingTool container for progressive tool disclosure.
 *
 * @deprecated Use [UnfoldingTools] instead. This annotation is retained for backward compatibility.
 * @see UnfoldingTools
 * @see LlmTool
 * @see com.embabel.agent.api.tool.progressive.UnfoldingTool.Factory.fromInstance
 */
@Deprecated(
    message = "Use @UnfoldingTools instead",
    replaceWith = ReplaceWith(
        "UnfoldingTools",
        "com.embabel.agent.api.annotation.UnfoldingTools"
    )
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatryoshkaTools(

    /**
     * Name of the MatryoshkaTool facade.
     * This is the tool name the LLM will see initially.
     */
    val name: String,

    /**
     * Description of the MatryoshkaTool.
     * Should explain what category of tools this contains
     * and instruct the LLM to invoke it to see specific options.
     */
    val description: String,

    /**
     * Whether to remove this tool after invocation.
     * Default is true - the facade is replaced by its inner tools.
     * Set to false to keep the facade available for re-invocation.
     */
    val removeOnInvoke: Boolean = true,

    /**
     * Name of the category parameter if using category-based selection.
     * Only used when `@LlmTool` methods have `category` specified.
     * Default is "category".
     */
    val categoryParameter: String = "category",

    /**
     * Optional usage notes to guide the LLM on when and how to use the child tools.
     * These notes are included in the context tool created when the MatryoshkaTool is invoked.
     *
     * Example:
     * ```java
     * @MatryoshkaTools(
     *     name = "music_search",
     *     description = "Tools for searching music data",
     *     childToolUsageNotes = "Try vector search first for semantic queries. " +
     *         "Fall back to text search for exact matches."
     * )
     * ```
     */
    val childToolUsageNotes: String = "",
)
