package com.example.AIResumeAnalyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class QdrantCollectionServiceImpl implements QdrantCollectionService {

    @Value("${qdrant.rest-url}")
    private String qdrantRestUrl;

    @Value("${voyage.dimension}")
    private int vectorDimension;

    @Override
    public String getCollectionName(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        String safeUsername = username
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_");

        return "resumes_" + safeUsername;
    }

    @Override
    public void createCollectionIfNotExists(String username) {

        String collectionName = getCollectionName(username);

        WebClient webClient = WebClient.builder()
                .baseUrl(qdrantRestUrl)
                .build();

        Boolean exists = webClient.get()
                .uri("/collections/{collectionName}/exists", collectionName)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object resultObject = response.get("result");

                    if (resultObject instanceof Map<?, ?> resultMap) {
                        return Boolean.TRUE.equals(resultMap.get("exists"));
                    }

                    return false;
                })
                .onErrorReturn(false)
                .block();

        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        Map<String, Object> requestBody = Map.of(
                "vectors", Map.of(
                        "size", vectorDimension,
                        "distance", "Cosine"
                )
        );

        webClient.put()
                .uri("/collections/{collectionName}", collectionName)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new IllegalStateException(
                                        "Unable to create Qdrant collection: " + body
                                ))
                )
                .bodyToMono(String.class)
                .block();
    }
}

//Qdrant requires creating a collection before adding vectors, and the configured vector size must
// match the embedding model’s output dimension.