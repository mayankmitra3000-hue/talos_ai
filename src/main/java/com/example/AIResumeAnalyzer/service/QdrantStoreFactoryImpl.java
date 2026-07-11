package com.example.AIResumeAnalyzer.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QdrantStoreFactoryImpl implements QdrantStoreFactory {

    private final QdrantCollectionService collectionService;

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.grpc-port}")
    private int qdrantGrpcPort;

    @Override
    public EmbeddingStore<TextSegment> createStore(String username) {

        collectionService.createCollectionIfNotExists(username);

        String collectionName =
                collectionService.getCollectionName(username);

        return QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantGrpcPort)
                .collectionName(collectionName)
                .build();
    }
}