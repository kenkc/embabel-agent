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
package com.embabel.agent.rag.pipeline

import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.event.AgentProcessRagEvent
import com.embabel.agent.event.RagEventListener
import com.embabel.agent.event.RagRequestReceivedEvent
import com.embabel.agent.event.RagResponseEvent
import com.embabel.agent.rag.*
import com.embabel.agent.rag.pipeline.event.InitialRequestRagPipelineEvent
import com.embabel.agent.rag.pipeline.event.InitialResponseRagPipelineEvent
import org.slf4j.LoggerFactory

/**
 * Decorates a Rag Service with an enhancement pipeline.
 */
class PipelinedRagServiceEnhancer(
    val ragServiceEnhancerProperties: RagServiceEnhancerProperties = RagServiceEnhancerProperties(),
) : RagServiceEnhancer {

    private val logger = LoggerFactory.getLogger(PipelinedRagServiceEnhancer::class.java)

    override fun create(
        operationContext: OperationContext,
        delegate: RagService,
        listener: RagEventListener,
    ): RagService {
        var listenerToUse = listener
        if (operationContext is ActionContext) {
            // For action contexts, we also want to send events to the process listener
            listenerToUse += { ragEvent ->
                operationContext.processContext.onProcessEvent(
                    AgentProcessRagEvent(operationContext.agentProcess, ragEvent)
                )
            }
        }
        return PipelinedRagService(
            operationContext = operationContext,
            delegate = delegate,
            listener = listenerToUse,
        )
    }

    private inner class PipelinedRagService(
        private val operationContext: OperationContext,
        private val delegate: RagService,
        private val listener: RagEventListener,
    ) : RagService {

        override val name
            get() = "pipelined(${delegate.name})"

        override val description
            get() = "Pipelined RAG service wrapping ${delegate.name}: ${delegate.description}"

        private fun hydeQuery(
            ragRequest: RagRequest,
            hyDE: HyDE,
        ): String {
            val hydeQuery = operationContext
                .ai()
                .withLlm(ragServiceEnhancerProperties.compressionLlm)
                .generateText(
                    """
                    Given the following request, generate a plausible hypothetical
                    answer.
                    Don't worry if the answer isn't accurate; just make it a reasonable
                    example of an answer to the query.
                    The answer should be at most ${ragRequest.hyDE?.wordCount ?: 50} words.

                    REQUEST:
                    ${ragRequest.query}

                    CONTEXT FOR THE ANSWER:
                    ${hyDE.context}
                """.trimIndent()
                )
            logger.info("{} -> Generated HyDE query: {}", ragRequest.query, hydeQuery)
            return hydeQuery
        }

        override fun search(ragRequest: RagRequest): RagResponse {
            listener.onRagEvent(RagRequestReceivedEvent(ragRequest))
            logger.info("Performing initial rag search for {} using RagService {}", ragRequest, delegate.name)
            val initialRequest = ragRequest.copy(
                query = ragRequest.hyDE?.let { hydeQuery(ragRequest, it) } ?: ragRequest.query,
                topK = ragRequest.topK * 2,
                similarityThreshold = ragRequest.similarityThreshold / 2,
            )
            listener.onRagEvent(InitialRequestRagPipelineEvent(initialRequest, delegate.name))

            // Return to initial request
            val initialResponse = delegate.search(initialRequest)
                .copy(request = ragRequest)
            listener.onRagEvent(InitialResponseRagPipelineEvent(initialResponse, delegate.name))

            val pipeline = AdaptivePipelineRagResponseEnhancer(
                enhancers = buildList {
                    add(DeduplicatingEnhancer)
                    if (ragRequest.compressionConfig.enabled) {
                        add(
                            PromptedContextualCompressionEnhancer(
                                operationContext,
                                ragServiceEnhancerProperties.compressionLlm,
                                ragServiceEnhancerProperties.maxConcurrency,
                            )
                        )
                    }
                    add(RerankingEnhancer(operationContext, ragServiceEnhancerProperties.rerankingLlm))
                    add(FilterEnhancer)
                },
                listener = listener,
            )
            val enhancedRagResponse = pipeline.enhance(initialResponse)
            listener.onRagEvent(RagResponseEvent(enhancedRagResponse))
            logger.info(
                "Final enhanced rag response has {} results: {} chunks, {} other content elements, {} entities",
                enhancedRagResponse.results.size,
                enhancedRagResponse.results.count { it.match is Chunk },
                enhancedRagResponse.results.count { it.match is ContentElement && it.match !is Chunk },
                enhancedRagResponse.results.count { it.match is RetrievableEntity },
            )
            return enhancedRagResponse
        }

        override fun infoString(
            verbose: Boolean?,
            indent: Int,
        ): String {
            return "PipelinedRagService wrapping ${delegate.infoString(verbose, indent + 2)}"
        }
    }
}
