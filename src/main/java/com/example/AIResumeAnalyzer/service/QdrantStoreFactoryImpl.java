package com.example.AIResumeAnalyzer.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QdrantStoreFactoryImpl
        implements QdrantStoreFactory {

    private final QdrantCollectionService collectionService;
    private final String qdrantHost;
    private final int qdrantGrpcPort;
    private final String qdrantApiKey;
    private final boolean qdrantUseTls;

    public QdrantStoreFactoryImpl(
            QdrantCollectionService collectionService,

            @Value("${qdrant.host}")
            String qdrantHost,

            @Value("${qdrant.grpc-port}")
            int qdrantGrpcPort,

            @Value("${qdrant.api-key}")
            String qdrantApiKey,

            @Value("${qdrant.use-tls}")
            boolean qdrantUseTls
    ) {
        this.collectionService = collectionService;
        this.qdrantHost = qdrantHost;
        this.qdrantGrpcPort = qdrantGrpcPort;
        this.qdrantApiKey = qdrantApiKey;
        this.qdrantUseTls = qdrantUseTls;
    }

    @Override
    public EmbeddingStore<TextSegment> createStore(
            String username
    ) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        collectionService.createCollectionIfNotExists(
                username
        );

        String collectionName =
                collectionService.getCollectionName(
                        username
                );

        return QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantGrpcPort)
                .apiKey(qdrantApiKey)
                .useTls(qdrantUseTls)
                .collectionName(collectionName)
                .build();
    }
}