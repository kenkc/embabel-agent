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
package com.embabel.agent.api.common.support

import com.embabel.agent.api.common.*
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupRequirement
import com.embabel.agent.core.Verbosity
import com.embabel.agent.core.support.safelyGetToolCallbacks
import com.embabel.agent.experimental.primitive.Determination
import com.embabel.agent.prompt.element.ContextualPromptElement
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.tools.agent.AgentToolCallback
import com.embabel.agent.tools.agent.Handoffs
import com.embabel.agent.tools.agent.PromptedTextCommunicator
import com.embabel.chat.Message
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.core.types.ZeroToOne
import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.ToolCallback

/**
 * Uses the platform's LlmOperations to execute the prompt.
 * All prompt running ends up through here.
 */
internal data class OperationContextPromptRunner(
    private val context: OperationContext,
    private val interactionId: InteractionId? = null,
    override val llm: LlmOptions,
    override val messages: List<Message> = emptyList(),
    override val toolGroups: Set<ToolGroupRequirement>,
    override val toolObjects: List<ToolObject>,
    override val promptContributors: List<PromptContributor>,
    private val contextualPromptContributors: List<ContextualPromptElement>,
    override val generateExamples: Boolean?,
    private val otherToolCallbacks: List<ToolCallback> = emptyList(),
) : PromptRunner {

    val action = (context as? ActionContext)?.action

    private fun idForPrompt(
        messages: List<Message>,
        outputClass: Class<*>,
    ): InteractionId {
        return InteractionId("${context.operation.name}-${outputClass.name}")
    }

    override fun withInteractionId(interactionId: InteractionId): PromptRunner =
        copy(interactionId = interactionId)

    override fun withMessages(messages: List<Message>): PromptRunner =
        copy(messages = this.messages + messages)

    override fun <T> createObject(
        messages: List<Message>,
        outputClass: Class<T>,
    ): T {
        return context.processContext.createObject(
            messages = this.messages + messages,
            interaction = LlmInteraction(
                llm = llm,
                toolGroups = this.toolGroups + toolGroups,
                toolCallbacks = safelyGetToolCallbacks(toolObjects) + otherToolCallbacks,
                promptContributors = promptContributors + contextualPromptContributors.map {
                    it.toPromptContributor(
                        context
                    )
                },
                id = interactionId ?: idForPrompt(messages, outputClass),
                generateExamples = generateExamples,
            ),
            outputClass = outputClass,
            agentProcess = context.processContext.agentProcess,
            action = action,
        )
    }

    override fun <T> createObjectIfPossible(
        messages: List<Message>,
        outputClass: Class<T>,
    ): T? {
        val result = context.processContext.createObjectIfPossible<T>(
            messages = messages,
            interaction = LlmInteraction(
                llm = llm,
                toolGroups = this.toolGroups + toolGroups,
                toolCallbacks = safelyGetToolCallbacks(toolObjects) + otherToolCallbacks,
                promptContributors = promptContributors + contextualPromptContributors.map {
                    it.toPromptContributor(
                        context
                    )
                },
                id = interactionId ?: idForPrompt(messages, outputClass),
                generateExamples = generateExamples,
            ),
            outputClass = outputClass,
            agentProcess = context.processContext.agentProcess,
            action = action,
        )
        if (result.isFailure) {
            loggerFor<OperationContextPromptRunner>().warn(
                "Failed to create object of type {} with messages {}: {}",
                outputClass.name,
                messages,
                result.exceptionOrNull()?.message,
            )
        }
        return result.getOrNull()
    }

    override fun evaluateCondition(
        condition: String,
        context: String,
        confidenceThreshold: ZeroToOne,
    ): Boolean {
        val prompt =
            """
            Evaluate this condition given the context.
            Return "result": whether you think it is true, your confidence level from 0-1,
            and an explanation of what you base this on.

            # Condition
            $condition

            # Context
            $context
            """.trimIndent()
        val determination = createObject(
            prompt = prompt,
            outputClass = Determination::class.java,
        )
        loggerFor<OperationContextPromptRunner>().info(
            "Condition {}: determination from {} was {}",
            condition,
            llm.criteria,
            determination,
        )
        return determination.result && determination.confidence >= confidenceThreshold
    }

    override fun withTemplate(templateName: String): TemplateOperations {
        return TemplateOperations(
            templateName = templateName,
            promptRunnerOperations = this,
            templateRenderer = context.agentPlatform().platformServices.templateRenderer,
        )
    }

    override fun withLlm(llm: LlmOptions): PromptRunner =
        copy(llm = llm)

    override fun withToolGroup(toolGroup: ToolGroupRequirement): PromptRunner =
        copy(toolGroups = this.toolGroups + toolGroup)

    override fun withToolGroup(toolGroup: ToolGroup): PromptRunner =
        copy(otherToolCallbacks = otherToolCallbacks + toolGroup.toolCallbacks)

    override fun withToolObject(toolObject: ToolObject): PromptRunner =
        copy(toolObjects = this.toolObjects + toolObject)

    override fun withHandoffs(vararg outputTypes: Class<*>): PromptRunner {
        val handoffs = Handoffs(
            autonomy = context.agentPlatform().platformServices.autonomy(),
            outputTypes = outputTypes.toList(),
            applicationName = context.agentPlatform().name,
        )
        return copy(
            otherToolCallbacks = this.otherToolCallbacks + handoffs.toolCallbacks,
        )
    }

    override fun withSubagents(
        vararg subagents: Subagent,
    ): PromptRunner {
        val newCallbacks = subagents.map { subagent ->
            val agent = subagent.resolve(context.agentPlatform())
            AgentToolCallback(
                autonomy = context.agentPlatform().platformServices.autonomy(),
                agent = agent,
                textCommunicator = PromptedTextCommunicator,
                objectMapper = context.agentPlatform().platformServices.objectMapper,
                inputType = subagent.inputClass,
                processOptionsCreator = { agentProcess ->
                    val blackboard = agentProcess.processContext.blackboard.spawn()
                    loggerFor<OperationContextPromptRunner>().info(
                        "Creating subagent process for {} with blackboard {}",
                        agent.name,
                        blackboard,
                    )
                    ProcessOptions(
                        verbosity = Verbosity(showPrompts = true),
                        blackboard = blackboard,
                    )
                },
            )
        }
        return copy(
            otherToolCallbacks = this.otherToolCallbacks + newCallbacks,
        )
    }

    override fun withPromptContributors(promptContributors: List<PromptContributor>): PromptRunner =
        copy(promptContributors = this.promptContributors + promptContributors)

    override fun withContextualPromptContributors(
        contextualPromptContributors: List<ContextualPromptElement>,
    ): PromptRunner =
        copy(contextualPromptContributors = this.contextualPromptContributors + contextualPromptContributors)

    override fun withGenerateExamples(generateExamples: Boolean): PromptRunner =
        copy(generateExamples = generateExamples)

    override fun <T> creating(outputClass: Class<T>): ObjectCreator<T> {
        return PromptRunnerObjectCreator(
            promptRunner = this,
            outputClass = outputClass,
            objectMapper = context.agentPlatform().platformServices.objectMapper,
        )
    }
}
