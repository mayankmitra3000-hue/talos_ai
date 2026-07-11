package com.example.AIResumeAnalyzer.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface QdrantStoreFactory {

    EmbeddingStore<TextSegment> createStore(String username);

}