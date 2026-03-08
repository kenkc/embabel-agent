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
package com.embabel.agent.api.common;

import com.embabel.chat.AssistantMessage;
import com.embabel.chat.Conversation;
import com.embabel.common.textio.template.TemplateRenderer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Java tests for {@link PromptRunner.Rendering#respond} to verify it
 * always returns an {@link AssistantMessage} from Java callers.
 */
class RenderingRespondJavaTest {

    /**
     * Minimal Rendering implementation for testing the default respond() method.
     */
    static class TestRendering implements PromptRunner.Rendering {

        private final boolean shouldThrow;

        TestRendering(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }

        @NotNull
        @Override
        public PromptRunner.Rendering withTemplateRenderer(@NotNull TemplateRenderer templateRenderer) {
            return this;
        }

        @NotNull
        @Override
        public <T> T createObject(@NotNull Class<T> outputClass, @NotNull Map<String, ?> model) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public String generateText(@NotNull Map<String, ?> model) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public AssistantMessage respondWithSystemPrompt(
                @NotNull Conversation conversation,
                @NotNull Map<String, ?> model
        ) {
            if (shouldThrow) {
                throw new RuntimeException("LLM error");
            }
            return new AssistantMessage("success response");
        }

        @NotNull
        @Override
        public AssistantMessage respondWithTrigger(
                @NotNull Conversation conversation,
                @NotNull String triggerPrompt,
                @NotNull Map<String, ?> model
        ) {
            throw new UnsupportedOperationException();
        }
    }

    private Conversation mockConversation() {
        Conversation conversation = mock(Conversation.class);
        when(conversation.getMessages()).thenReturn(List.of());
        return conversation;
    }

    @Test
    void respondReturnsAssistantMessageOnSuccess() {
        var rendering = new TestRendering(false);
        var conversation = mockConversation();

        AssistantMessage result = rendering.respond(
                conversation,
                Map.of(),
                error -> new AssistantMessage("fallback")
        );

        assertInstanceOf(AssistantMessage.class, result);
        assertEquals("success response", result.getContent());
    }

    @Test
    void respondReturnsAssistantMessageFromOnFailure() {
        var rendering = new TestRendering(true);
        var conversation = mockConversation();

        AssistantMessage result = rendering.respond(
                conversation,
                Map.of(),
                error -> new AssistantMessage("Handled: " + error.getMessage())
        );

        assertInstanceOf(AssistantMessage.class, result);
        assertEquals("Handled: LLM error", result.getContent());
    }

    @Test
    void respondOnFailureReceivesOriginalException() {
        var rendering = new TestRendering(true);
        var conversation = mockConversation();
        final Throwable[] captured = {null};

        rendering.respond(
                conversation,
                Map.of(),
                error -> {
                    captured[0] = error;
                    return new AssistantMessage("error");
                }
        );

        assertNotNull(captured[0]);
        assertInstanceOf(RuntimeException.class, captured[0]);
        assertEquals("LLM error", captured[0].getMessage());
    }
}
