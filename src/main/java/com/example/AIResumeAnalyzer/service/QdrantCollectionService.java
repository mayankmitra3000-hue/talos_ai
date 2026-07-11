package com.example.AIResumeAnalyzer.service;

public interface QdrantCollectionService {

    String getCollectionName(String username);

    void createCollectionIfNotExists(String username);
}