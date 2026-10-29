package com.example.AiInternshipAllocation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class QdrantCollectionServiceImpl
        implements QdrantCollectionService {

    private final WebClient qdrantWebClient;
    private final int vectorDimension;

    public QdrantCollectionServiceImpl(
            WebClient.Builder webClientBuilder,

            @Value("${qdrant.rest-url}")
            String qdrantRestUrl,

            @Value("${qdrant.api-key}")
            String qdrantApiKey,

            @Value("${voyage.dimension}")
            int vectorDimension
    ) {
        this.qdrantWebClient =
                webClientBuilder
                        .baseUrl(qdrantRestUrl)
                        .defaultHeader(
                                "api-key",
                                qdrantApiKey
                        )
                        .build();

        this.vectorDimension = vectorDimension;
    }

    @Override
    public String getCollectionName(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        String safeUsername =
                username
                        .trim()
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9_]",
                                "_"
                        );

        return "resumes_" + safeUsername;
    }

    @Override
    public void createCollectionIfNotExists(
            String username
    ) {
        String collectionName =
                getCollectionName(username);

        Boolean exists =
                qdrantWebClient
                        .get()
                        .uri(
                                "/collections/{collectionName}/exists",
                                collectionName
                        )
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                response ->
                                        response
                                                .bodyToMono(String.class)
                                                .map(body ->
                                                        new IllegalStateException(
                                                                "Unable to check Qdrant collection: "
                                                                        + body
                                                        )
                                                )
                        )
                        .bodyToMono(Map.class)
                        .map(response -> {

                            Object resultObject =
                                    response.get("result");

                            if (resultObject
                                    instanceof Map<?, ?> resultMap) {

                                return Boolean.TRUE.equals(
                                        resultMap.get("exists")
                                );
                            }

                            return false;
                        })
                        .block();

        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        Map<String, Object> requestBody =
                Map.of(
                        "vectors",
                        Map.of(
                                "size",
                                vectorDimension,

                                "distance",
                                "Cosine"
                        )
                );

        String response =
                qdrantWebClient
                        .put()
                        .uri(
                                "/collections/{collectionName}",
                                collectionName
                        )
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                errorResponse ->
                                        errorResponse
                                                .bodyToMono(String.class)
                                                .map(body ->
                                                        new IllegalStateException(
                                                                "Unable to create Qdrant collection: "
                                                                        + body
                                                        )
                                                )
                        )
                        .bodyToMono(String.class)
                        .block();

        System.out.println(
                "Created Qdrant collection: "
                        + collectionName
        );

        System.out.println(
                "Qdrant response: "
                        + response
        );
    }
}