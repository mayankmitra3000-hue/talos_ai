package com.example.AIResumeAnalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GroqServiceImpl implements GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public GroqServiceImpl() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.groq.com")
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
                5. ATS Compatibility(Yes/No/Could Be Better)
                6. Improvement Suggestions
                
                Resume:
                """ + resumeText;

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 1024
        );

        try {
            Map response = webClient.post()
                    .uri("/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            return message.get("content").toString();

        }
        catch (WebClientResponseException e) {
            System.out.println("Groq Error Body: " + e.getResponseBodyAsString());
            e.printStackTrace();
            return "Failed to analyze resume.";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Failed to analyze resume.";
        }
    }

    @Override
    public String atsScoreGeneration(String resumeText) {

        String prompt = """
            You are an expert ATS Resume Reviewer.

            Analyze the following resume and estimate its ATS friendliness.

            Return your answer in exactly this format:

            ATS Score: XX/100

            Keyword Optimization:
            - ...

            Grammar & Formatting:
            - ...

            ATS Compatibility:
            Excellent / Good / Average / Poor

            Scoring Guidelines:
            - Keywords (30 points)
            - Skills (20 points)
            - Formatting (15 points)
            - Grammar (10 points)
            - Experience & Achievements (15 points)
            - Readability (10 points)

            Resume:
            """ + resumeText;

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 1024
        );

        try {
            Map response = webClient.post()
                    .uri("/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            return message.get("content").toString();

        }
        catch (WebClientResponseException e) {
            System.out.println("Groq Error Body: " + e.getResponseBodyAsString());
            e.printStackTrace();
            return "Failed to analyze resume.";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Failed to analyze resume.";
        }
    }
}