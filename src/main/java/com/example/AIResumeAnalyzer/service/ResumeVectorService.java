package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeVectorService {

    @Qualifier("voyageDocumentEmbeddingModel")
    private final EmbeddingModel documentEmbeddingModel;

    private final QdrantStoreFactory qdrantStoreFactory;
    private final ResumeChunkingService resumeChunkService;

    public void indexResume(Resume resume) {

        if (resume == null) {
            throw new IllegalArgumentException("Resume cannot be null");
        }

        if (resume.getUser() == null) {
            throw new IllegalArgumentException(
                    "Resume must belong to a user"
            );
        }

        if (resume.getResumeText() == null
                || resume.getResumeText().isBlank()) {

            throw new IllegalArgumentException(
                    "Resume text cannot be empty"
            );
        }

        String username =
                resume.getUser().getUsername();

        EmbeddingStore<TextSegment> embeddingStore =
                qdrantStoreFactory.createStore(username);

        List<String> chunks =
                resumeChunkService.splitIntoChunks(
                        resume.getResumeText()
                );

        for (int index = 0; index < chunks.size(); index++) {

            Metadata metadata = Metadata.from(
                    "resumeId",
                    resume.getResumeId().toString()
            );

            metadata.put(
                    "fileName",
                    resume.getFileName()
            );

            metadata.put(
                    "username",
                    username
            );

            metadata.put(
                    "chunkNumber",
                    index
            );

            TextSegment segment = TextSegment.from(
                    chunks.get(index),
                    metadata
            );

            Embedding embedding =
                    documentEmbeddingModel
                            .embed(segment)
                            .content();

            embeddingStore.add(
                    embedding,
                    segment
            );
        }
    }
}