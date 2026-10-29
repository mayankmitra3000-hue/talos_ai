package com.example.AiInternshipAllocation.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class VoyageEmbeddingsConfig {

    @Value("${voyage.api.key}")
    private String voyageApiKey;

    @Value("${voyage.model}")
    private String voyageModel;

    /**
     * Used while storing resume chunks.
     */
    @Bean("voyageDocumentEmbeddingModel")
    public EmbeddingModel voyageDocumentEmbeddingModel() {

        return VoyageAiEmbeddingModel.builder()
                .apiKey(voyageApiKey)
                .modelName(voyageModel)
                .inputType("document")
                .truncation(true)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    /**
     * Used while searching using a job description.
     */
    @Bean("voyageQueryEmbeddingModel")
    public EmbeddingModel voyageQueryEmbeddingModel() {

        return VoyageAiEmbeddingModel.builder()
                .apiKey(voyageApiKey)
                .modelName(voyageModel)
                .inputType("query")
                .truncation(true)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}

//We use two model beans because Voyage optimizes document embeddings and query embeddings differently.