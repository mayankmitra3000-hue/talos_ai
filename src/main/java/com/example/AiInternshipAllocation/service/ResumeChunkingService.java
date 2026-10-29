package com.example.AiInternshipAllocation.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeChunkingService {

    private static final int CHUNK_SIZE = 1800;
    private static final int CHUNK_OVERLAP = 250;

    public List<String> splitIntoChunks(String text) {

        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleanedText = text
                .replace("\u0000", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        int start = 0;

        while (start < cleanedText.length()) {

            int end = Math.min(
                    start + CHUNK_SIZE,
                    cleanedText.length()
            );

            if (end < cleanedText.length()) {
                int paragraphEnd =
                        cleanedText.lastIndexOf("\n", end);

                int sentenceEnd =
                        cleanedText.lastIndexOf(". ", end);

                int betterEnd = Math.max(
                        paragraphEnd,
                        sentenceEnd
                );

                if (betterEnd > start + 500) {
                    end = betterEnd + 1;
                }
            }

            String chunk = cleanedText
                    .substring(start, end)
                    .trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= cleanedText.length()) {
                break;
            }

            start = Math.max(
                    end - CHUNK_OVERLAP,
                    start + 1
            );
        }

        return chunks;
    }
}