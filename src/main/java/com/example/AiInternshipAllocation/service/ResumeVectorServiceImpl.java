package com.example.AiInternshipAllocation.service;

import com.example.AiInternshipAllocation.entity.Resume;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ResumeVectorServiceImpl
        implements ResumeVectorService {

    private final EmbeddingModel documentEmbeddingModel;

    private final QdrantStoreFactory qdrantStoreFactory;

    private final ResumeChunkingService resumeChunkService;

    private final QdrantCollectionService qdrantCollectionService;

    private final WebClient qdrantWebClient;

    public ResumeVectorServiceImpl(
            @Qualifier("voyageDocumentEmbeddingModel")
            EmbeddingModel documentEmbeddingModel,

            QdrantStoreFactory qdrantStoreFactory,

            ResumeChunkingService resumeChunkService,

            QdrantCollectionService qdrantCollectionService,

            WebClient.Builder webClientBuilder,

            @Value("${qdrant.rest-url}")
            String qdrantRestUrl,

            @Value("${qdrant.api-key}")
            String qdrantApiKey
    ) {
        this.documentEmbeddingModel =
                documentEmbeddingModel;

        this.qdrantStoreFactory =
                qdrantStoreFactory;

        this.resumeChunkService =
                resumeChunkService;

        this.qdrantCollectionService =
                qdrantCollectionService;

        this.qdrantWebClient =
                webClientBuilder
                        .baseUrl(qdrantRestUrl)
                        .defaultHeader(
                                "api-key",
                                qdrantApiKey
                        )
                        .build();
    }

    /**
     * Splits the resume into chunks, creates an embedding
     * for every chunk and stores it in Qdrant.
     */
    @Override
    public void indexResume(Resume resume) {

        validateResume(resume);

        String username =
                resume.getUser().getUsername();

        /*
         * Creates or connects to the Qdrant collection
         * belonging to the current user.
         */
        EmbeddingStore<TextSegment> embeddingStore =
                qdrantStoreFactory.createStore(username);

        /*
         * Split the complete resume text into smaller chunks.
         */
        List<String> chunks =
                resumeChunkService.splitIntoChunks(
                        resume.getResumeText()
                );

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalStateException(
                    "No chunks were created from the resume"
            );
        }

        for (int index = 0;
             index < chunks.size();
             index++) {

            String chunkText = chunks.get(index);

            if (chunkText == null
                    || chunkText.isBlank()) {
                continue;
            }

            /*
             * Metadata stored with each vector.
             *
             * resumeId is deliberately stored as String.
             * Therefore, the delete filter must also use String.
             */
            Metadata metadata =
                    Metadata.from(
                            "resumeId",
                            String.valueOf(
                                    resume.getResumeId()
                            )
                    );

            metadata.put(
                    "fileName",
                    resume.getFileName()
            );

            metadata.put(
                    "username",
                    username
            );

            /*
             * Using index + 1 means chunk numbering starts
             * from 1 instead of 0.
             */
            metadata.put(
                    "chunkNumber",
                    index + 1
            );

            TextSegment segment =
                    TextSegment.from(
                            chunkText,
                            metadata
                    );

            /*
             * Generate a 1024-dimensional Voyage AI embedding.
             */
            Embedding embedding =
                    documentEmbeddingModel
                            .embed(segment)
                            .content();

            /*
             * Store the vector and corresponding text segment
             * in the user's Qdrant collection.
             */
            embeddingStore.add(
                    embedding,
                    segment
            );
        }

        System.out.println(
                "Successfully indexed "
                        + chunks.size()
                        + " chunks for resumeId: "
                        + resume.getResumeId()
        );
    }

    /**
     * Deletes all Qdrant points belonging to a particular resume.
     */
    @Override
    public void deleteResumeEmbeddings(
            Long resumeId,
            String username
    ) {
        validateDeleteRequest(
                resumeId,
                username
        );

        String collectionName =
                qdrantCollectionService
                        .getCollectionName(username);

        /*
         * This produces the following Qdrant filter:
         *
         * {
         *   "filter": {
         *     "must": [
         *       {
         *         "key": "resumeId",
         *         "match": {
         *           "value": "8"
         *         }
         *       },
         *       {
         *         "key": "username",
         *         "match": {
         *           "value": "mayank_m"
         *         }
         *       }
         *     ]
         *   }
         * }
         */
        Map<String, Object> requestBody =
                Map.of(
                        "filter",
                        Map.of(
                                "must",
                                List.of(
                                        Map.of(
                                                "key",
                                                "resumeId",

                                                "match",
                                                Map.of(
                                                        "value",
                                                        String.valueOf(
                                                                resumeId
                                                        )
                                                )
                                        ),

                                        Map.of(
                                                "key",
                                                "username",

                                                "match",
                                                Map.of(
                                                        "value",
                                                        username
                                                )
                                        )
                                )
                        )
                );

        String response =
                qdrantWebClient
                        .post()
                        .uri(
                                uriBuilder ->
                                        uriBuilder
                                                .path(
                                                        "/collections/{collectionName}/points/delete"
                                                )
                                                .queryParam(
                                                        "wait",
                                                        true
                                                )
                                                .build(
                                                        collectionName
                                                )
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

        System.out.println(
                "Deleted Qdrant chunks for resumeId: "
                        + resumeId
                        + ", username: "
                        + username
        );

        System.out.println(
                "Qdrant delete response: "
                        + response
        );
    }

    /**
     * Validates a resume before generating embeddings.
     */
    private void validateResume(Resume resume) {

        if (resume == null) {
            throw new IllegalArgumentException(
                    "Resume cannot be null"
            );
        }

        if (resume.getResumeId() == null) {
            throw new IllegalArgumentException(
                    "Resume must be saved before creating embeddings"
            );
        }

        if (resume.getUser() == null) {
            throw new IllegalArgumentException(
                    "Resume must belong to a user"
            );
        }

        if (resume.getUser().getUsername() == null
                || resume.getUser()
                .getUsername()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        if (resume.getResumeText() == null
                || resume.getResumeText().isBlank()) {

            throw new IllegalArgumentException(
                    "Resume text cannot be empty"
            );
        }

        if (resume.getFileName() == null
                || resume.getFileName().isBlank()) {

            throw new IllegalArgumentException(
                    "Resume file name cannot be empty"
            );
        }
    }

    /**
     * Validates input before deleting embeddings.
     */
    private void validateDeleteRequest(
            Long resumeId,
            String username
    ) {
        if (resumeId == null) {
            throw new IllegalArgumentException(
                    "Resume ID cannot be null"
            );
        }

        if (username == null
                || username.isBlank()) {

            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }
    }
}