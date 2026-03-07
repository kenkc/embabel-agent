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
package com.embabel.agent.api.tool.progressive;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.annotation.UnfoldingTools;
import com.embabel.agent.api.tool.Tool;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that UnfoldingTool factory methods are callable from Java
 * via static method syntax (i.e., {@code UnfoldingTool.method(...)}
 * not {@code UnfoldingTool.Companion.method(...)}).
 * <p>
 * Tests both the short-param convenience overloads (with defaults)
 * and the full-param versions.
 */
class UnfoldingToolJavaTest {

    // Test fixture: plain class with @LlmTool methods, no @UnfoldingTools annotation
    public static class PlainTools {
        @LlmTool(description = "Search for items")
        public String search(String query) {
            return "Results for: " + query;
        }

        @LlmTool(description = "Count items")
        public String count() {
            return "42";
        }
    }

    @UnfoldingTools(
        name = "annotated_tools",
        description = "Annotated tool group"
    )
    public static class AnnotatedTools {
        @LlmTool(description = "Do something")
        public String doSomething() {
            return "done";
        }
    }

    @Nested
    class OfTest {

        @Test
        void ofShortFormIsCallableAsStaticMethod() {
            Tool inner = Tool.create("inner", "Inner tool",
                Tool.InputSchema.empty(), input -> Tool.Result.text("result"));

            UnfoldingTool tool = UnfoldingTool.of(
                "my_tools",
                "My tools",
                List.of(inner)
            );

            assertEquals("my_tools", tool.getDefinition().getName());
            assertEquals("My tools", tool.getDefinition().getDescription());
            assertEquals(1, tool.getInnerTools().size());
            assertTrue(tool.getRemoveOnInvoke());
            assertNull(tool.getChildToolUsageNotes());
        }

        @Test
        void ofFullFormIsCallableAsStaticMethod() {
            Tool inner = Tool.create("inner", "Inner tool",
                Tool.InputSchema.empty(), input -> Tool.Result.text("result"));

            UnfoldingTool tool = UnfoldingTool.of(
                "my_tools",
                "My tools",
                List.of(inner),
                false,
                "Usage notes here"
            );

            assertEquals("my_tools", tool.getDefinition().getName());
            assertFalse(tool.getRemoveOnInvoke());
            assertEquals("Usage notes here", tool.getChildToolUsageNotes());
        }
    }

    @Nested
    class FromToolObjectTest {

        @Test
        void fromToolObjectShortFormIsCallableAsStaticMethod() {
            UnfoldingTool tool = UnfoldingTool.fromToolObject(
                new PlainTools(),
                "plain_tools",
                "Plain tool group"
            );

            assertEquals("plain_tools", tool.getDefinition().getName());
            assertEquals("Plain tool group", tool.getDefinition().getDescription());
            assertEquals(2, tool.getInnerTools().size());
            assertTrue(tool.getRemoveOnInvoke());
            assertNull(tool.getChildToolUsageNotes());

            List<String> names = tool.getInnerTools().stream()
                .map(t -> t.getDefinition().getName())
                .toList();
            assertTrue(names.contains("search"));
            assertTrue(names.contains("count"));
        }

        @Test
        void fromToolObjectFullFormIsCallableAsStaticMethod() {
            UnfoldingTool tool = UnfoldingTool.fromToolObject(
                new PlainTools(),
                "plain_tools",
                "Plain tool group",
                false,
                "Use search first"
            );

            assertFalse(tool.getRemoveOnInvoke());
            assertEquals("Use search first", tool.getChildToolUsageNotes());
        }

        @Test
        void fromToolObjectInnerToolsAreCallable() {
            UnfoldingTool tool = UnfoldingTool.fromToolObject(
                new PlainTools(),
                "tools",
                "Tools"
            );

            Tool searchTool = tool.getInnerTools().stream()
                .filter(t -> t.getDefinition().getName().equals("search"))
                .findFirst()
                .orElseThrow();

            Tool.Result result = searchTool.call("{\"query\": \"test\"}");

            assertInstanceOf(Tool.Result.Text.class, result);
            assertTrue(((Tool.Result.Text) result).getContent().contains("test"));
        }
    }

    @Nested
    class ByCategoryTest {

        @Test
        void byCategoryShortFormIsCallableAsStaticMethod() {
            Tool readTool = Tool.create("read", "Read data",
                Tool.InputSchema.empty(), input -> Tool.Result.text("read"));
            Tool writeTool = Tool.create("write", "Write data",
                Tool.InputSchema.empty(), input -> Tool.Result.text("wrote"));

            UnfoldingTool tool = UnfoldingTool.byCategory(
                "data_ops",
                "Data operations",
                Map.of(
                    "read", List.of(readTool),
                    "write", List.of(writeTool)
                )
            );

            assertEquals("data_ops", tool.getDefinition().getName());
            assertEquals(2, tool.getInnerTools().size());
        }

        @Test
        void byCategoryFullFormIsCallableAsStaticMethod() {
            Tool readTool = Tool.create("read", "Read data",
                Tool.InputSchema.empty(), input -> Tool.Result.text("read"));

            UnfoldingTool tool = UnfoldingTool.byCategory(
                "data_ops",
                "Data operations",
                Map.of("read", List.of(readTool)),
                "type",
                false,
                "Pick a category"
            );

            assertEquals("data_ops", tool.getDefinition().getName());
            assertFalse(tool.getRemoveOnInvoke());
            assertEquals("Pick a category", tool.getChildToolUsageNotes());
        }
    }

    @Nested
    class FromInstanceTest {

        @Test
        void fromInstanceShortFormIsCallableAsStaticMethod() {
            UnfoldingTool tool = UnfoldingTool.fromInstance(new AnnotatedTools());

            assertEquals("annotated_tools", tool.getDefinition().getName());
            assertEquals(1, tool.getInnerTools().size());
        }
    }

    @Nested
    class SafelyFromInstanceTest {

        @Test
        void safelyFromInstanceShortFormIsCallableAsStaticMethod() {
            UnfoldingTool tool = UnfoldingTool.safelyFromInstance(new AnnotatedTools());

            assertNotNull(tool);
            assertEquals("annotated_tools", tool.getDefinition().getName());
        }

        @Test
        void safelyFromInstanceReturnsNullForNonAnnotatedClass() {
            UnfoldingTool tool = UnfoldingTool.safelyFromInstance(new PlainTools());

            assertNull(tool);
        }
    }
}
