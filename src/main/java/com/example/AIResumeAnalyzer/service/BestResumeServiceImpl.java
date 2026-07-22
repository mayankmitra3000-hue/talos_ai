package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BestResumeServiceImpl implements BestResumeService {

    private final EmbeddingModel queryEmbeddingModel;
    private final QdrantStoreFactory qdrantStoreFactory;
    private final ResumeService resumeService;

    @Value("${rag.maximum-results:20}")
    private int maximumResults;

    @Value("${rag.minimum-score:0.45}")
    private double minimumScore;

    public BestResumeServiceImpl(
            @Qualifier("voyageQueryEmbeddingModel")
            EmbeddingModel queryEmbeddingModel,
            QdrantStoreFactory qdrantStoreFactory,
            ResumeService resumeService
    ) {
        this.queryEmbeddingModel = queryEmbeddingModel;
        this.qdrantStoreFactory = qdrantStoreFactory;
        this.resumeService = resumeService;
    }

    @Override
    public List<Map<String, Object>> findBestResumes(
            String username,
            String jobDescription
    ) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException(
                    "Job description cannot be empty"
            );
        }

        /*
         * Creates or connects to this user's
         * Qdrant resume collection.
         */
        EmbeddingStore<TextSegment> embeddingStore =
                qdrantStoreFactory.createStore(username);

        /*
         * Generate query embedding using Voyage AI.
         */
        Embedding queryEmbedding =
                queryEmbeddingModel
                        .embed(jobDescription)
                        .content();

        /*
         * Search matching chunks in Qdrant.
         */
        EmbeddingSearchRequest searchRequest =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maximumResults)
                        .minScore(minimumScore)
                        .build();

        EmbeddingSearchResult<TextSegment> searchResult =
                embeddingStore.search(searchRequest);

        /*
         * One resume can have multiple matching chunks.
         *
         * bestScores:
         * resumeId -> highest similarity score
         *
         * matchedChunks:
         * resumeId -> all useful matching chunks
         */
        Map<Long, Double> bestScores =
                new LinkedHashMap<>();

        Map<Long, Set<String>> matchedChunks =
                new LinkedHashMap<>();

        for (EmbeddingMatch<TextSegment> match
                : searchResult.matches()) {

            TextSegment segment = match.embedded();

            if (segment == null) {
                continue;
            }

            String resumeIdText =
                    segment.metadata()
                            .getString("resumeId");

            if (resumeIdText == null
                    || resumeIdText.isBlank()) {
                continue;
            }

            Long resumeId;

            try {
                resumeId = Long.valueOf(resumeIdText);
            } catch (NumberFormatException exception) {
                continue;
            }

            /*
             * Keep the highest-scoring chunk score
             * for each resume.
             */
            bestScores.merge(
                    resumeId,
                    match.score(),
                    Math::max
            );

            /*
             * Save unique matching chunks.
             */
            matchedChunks
                    .computeIfAbsent(
                            resumeId,
                            key -> new LinkedHashSet<>()
                    )
                    .add(segment.text());
        }

        List<Map<String, Object>> results =
                new ArrayList<>();

        for (Map.Entry<Long, Double> entry
                : bestScores.entrySet()) {

            Long resumeId = entry.getKey();
            Double similarityScore = entry.getValue();

            try {

                /*
                 * Get complete resume details from MySQL.
                 * This also verifies that the resume
                 * belongs to the logged-in user.
                 */
                Resume resume =
                        resumeService
                                .getResumeByIdAndUsername(
                                        resumeId,
                                        username
                                );

                String combinedMatchedContent =
                        String.join(
                                "\n\n",
                                matchedChunks.getOrDefault(
                                        resumeId,
                                        Set.of()
                                )
                        );

                Map<String, Object> result =
                        new LinkedHashMap<>();

                result.put("resume", resume);
                result.put("score", similarityScore);

                result.put(
                        "percentage",
                        Math.round(similarityScore * 100)
                );

                result.put(
                        "matchedContent",
                        combinedMatchedContent
                );

                results.add(result);

            } catch (Exception ignored) {
                /*
                 * Ignore stale Qdrant vectors if their
                 * corresponding MySQL resume was deleted.
                 */
            }
        }

        /*
         * Sort resumes from highest to lowest score.
         */
        results.sort(
                Comparator.comparingDouble(
                        result ->
                                -((Number) result.get("score"))
                                        .doubleValue()
                )
        );

        return results;
    }
}