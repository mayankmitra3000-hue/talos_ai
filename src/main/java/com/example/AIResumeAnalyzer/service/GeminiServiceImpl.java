package com.example.AIResumeAnalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public GeminiServiceImpl() {

        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    @Override
    public String analyzeResume(String resumeText) {

        String prompt = """
                Analyze this resume and provide:
                
                1. Resume Summary
                2. Skills Detected
                3. Strengths
                4. Weaknesses
                5. ATS Score out of 100
                6. Improvement Suggestions
                
                Resume:
                """ + resumeText;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        try {

            Map response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/gemini-2.5-flash:generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");

            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");

// ✅ Loop through parts and find the actual text (skip thinking blocks)
            String resultText = parts.stream()
                    .filter(part -> part.get("text") != null)
                    .map(part -> part.get("text").toString())
                    .findFirst()
                    .orElse("No response received.");

            return resultText;

        } catch (Exception e) {

            e.printStackTrace();

            return "Failed to analyze resume.";
        }
    }
}
