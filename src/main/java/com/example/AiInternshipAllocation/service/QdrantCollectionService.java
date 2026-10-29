package com.example.AiInternshipAllocation.service;

public interface QdrantCollectionService {

    String getCollectionName(String username);

    void createCollectionIfNotExists(String username);
}