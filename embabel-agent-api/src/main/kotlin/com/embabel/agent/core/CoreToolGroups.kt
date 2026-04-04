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
package com.embabel.agent.core

/**
 * Core tool groups exposed by the platform
 * These should be supported in any AgentPlatform instance.
 */
object CoreToolGroups {

    const val WEB = "web"

    val WEB_DESCRIPTION = ToolGroupDescription(
        description = "Search the web, fetch URLs, and look up Wikipedia articles. " +
                "Use this when the user asks to search online, find information on the web, " +
                "look something up, fetch a webpage, or check Wikipedia.",
        role = WEB,
        childToolUsageNotes = """
            brave_web_search: General web search. Use for current events, facts, news, or any web lookup.
            fetch: Fetch and extract content from a specific URL. Use when the user provides a URL.
            search_wikipedia / get_article / get_summary: Wikipedia lookups. Use when the user asks
            about Wikipedia specifically or wants encyclopedic/reference information on a topic.
        """.trimIndent(),
    )

    const val MATH = "math"

    val MATH_DESCRIPTION = ToolGroupDescription(
        description = "Math tools: use when you need to perform calculations",
        role = MATH,
    )

    const val MAPS = "maps"

    val MAPS_DESCRIPTION = ToolGroupDescription(
        description = "Mapping and geolocation tools. Use when the user asks about places, " +
                "directions, nearby locations, or anything geographic.",
        role = MAPS,
    )

    const val GITHUB = "github"

    val GITHUB_DESCRIPTION = ToolGroupDescription(
        description = "GitHub repository management — issues, pull requests, code search, and more. " +
                "Use this when the user asks about GitHub issues, PRs, repositories, commits, or code on GitHub. " +
                "IMPORTANT: GitHub issues are NOT workspace tasks — use this tool for GitHub, not the task system.",
        role = GITHUB,
        childToolUsageNotes = """
            ISSUES: list_issues to browse, issue_read to view details, issue_write to create/update,
            add_issue_comment to comment, search_issues for advanced search.
            PULL REQUESTS: list_pull_requests, pull_request_read, create_pull_request, merge_pull_request.
            CODE: search_code for searching across repositories, get_file_contents to read files.
            Always specify the owner/repo (e.g. 'embabel/embabel-agent') when calling GitHub tools.
        """.trimIndent(),
    )

    const val BROWSER_AUTOMATION = "browser_automation"

    val BROWSER_AUTOMATION_DESCRIPTION = ToolGroupDescription(
        description = "Browser automation — navigate pages, take screenshots, click elements, " +
                "fill forms. Use for testing web apps or interacting with web pages programmatically.",
        role = BROWSER_AUTOMATION,
    )
}
