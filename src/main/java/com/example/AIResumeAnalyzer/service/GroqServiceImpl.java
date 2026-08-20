package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

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
                "model", "openai/gpt-oss-120b",
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
                "model", "openai/gpt-oss-120b",
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
    public String compareResumesWithJob(
            String jobDescription,
            List<Map<String, Object>> matchingResumes
    ) {

        if (jobDescription == null
                || jobDescription.isBlank()) {

            throw new IllegalArgumentException(
                    "Job description cannot be empty"
            );
        }

        if (matchingResumes == null
                || matchingResumes.isEmpty()) {

            return "No suitable resumes were found for this job description.";
        }

        StringBuilder resumesContext =
                new StringBuilder();

        int retrievalRank = 1;

        for (Map<String, Object> result
                : matchingResumes) {

            Resume resume =
                    (Resume) result.get("resume");

            Number percentage =
                    (Number) result.get("percentage");

            String matchedContent =
                    String.valueOf(
                            result.get("matchedContent")
                    );

            resumesContext
                    .append("\n\n====================================")
                    .append("\nRETRIEVED RESUME ")
                    .append(retrievalRank++)
                    .append("\n====================================")
                    .append("\nResume ID: ")
                    .append(resume.getResumeId())
                    .append("\nFile name: ")
                    .append(resume.getFileName())
                    .append("\nVector similarity: ")
                    .append(percentage)
                    .append("%")
                    .append("\nRelevant retrieved content:\n")
                    .append(matchedContent);
        }

        String prompt = """
            You are an experienced technical recruiter and ATS evaluator.

            Compare the provided resumes against the supplied job description.

            The resumes were initially retrieved using vector similarity.
            Vector similarity is only a retrieval score and must not be treated
            as the final suitability or ATS score.

            Evaluate each candidate based only on the information available
            in the retrieved resume content.

            Return the answer in the following exact structure:

            BEST MATCH

            Resume ID:
            File Name:
            Suitability Score:
            Recommendation:
            Explanation:

            RANKING OF ALL RESUMES

            For every resume provide:

            Rank:
            Resume ID:
            File Name:
            Suitability Score:
            Matching Required Skills:
            Matching Preferred Skills:
            Relevant Experience:
            Education Match:
            Missing Skills or Qualifications:
            Strengths:
            Weaknesses:
            Final Assessment:

            FINAL HIRING RECOMMENDATION

            Mention:
            - The strongest candidate
            - Why this candidate ranks first
            - Important concerns before selection
            - Suggested interview focus areas

            Important rules:

            1. Do not invent skills, education or experience.
            2. Do not assume missing information.
            3. Required skills should have greater importance than preferred skills.
            4. Experience, education and qualifications must also influence ranking.
            5. Suitability Score must be out of 100.
            6. Keep the explanation clear and recruiter-friendly.
            7. Do not use markdown tables.

            JOB DESCRIPTION:

            %s

            RETRIEVED RESUMES:

            %s
            """.formatted(
                jobDescription,
                resumesContext
        );

        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        requestBody.put(
                "model", "openai/gpt-oss-120b"
        );

        requestBody.put(
                "messages",
                List.of(
                        Map.of(
                                "role",
                                "system",
                                "content",
                                "You are an expert recruiter and resume evaluator."
                        ),
                        Map.of(
                                "role",
                                "user",
                                "content",
                                prompt
                        )
                )
        );

        requestBody.put("temperature", 0.2);
        requestBody.put("max_tokens", 3000);

        try {

            Map<String, Object> response =
                    webClient.post()
                            .uri("/openai/v1/chat/completions")
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

            if (response == null) {
                throw new IllegalStateException(
                        "Empty response received from Groq"
                );
            }

            Object choicesObject =
                    response.get("choices");

            if (!(choicesObject instanceof List<?> choices)
                    || choices.isEmpty()) {

                throw new IllegalStateException(
                        "No response choices received from Groq"
                );
            }

            Object firstChoiceObject =
                    choices.get(0);

            if (!(firstChoiceObject
                    instanceof Map<?, ?> firstChoice)) {

                throw new IllegalStateException(
                        "Invalid Groq response format"
                );
            }

            Object messageObject =
                    firstChoice.get("message");

            if (!(messageObject
                    instanceof Map<?, ?> message)) {

                throw new IllegalStateException(
                        "Groq response message was missing"
                );
            }

            Object content =
                    message.get("content");

            if (content == null) {
                throw new IllegalStateException(
                        "Groq response content was missing"
                );
            }

            return content.toString();

        } catch (WebClientResponseException exception) {

            throw new IllegalStateException(
                    "Groq API error: "
                            + exception.getStatusCode()
                            + " - "
                            + exception.getResponseBodyAsString(),
                    exception
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to compare resumes using Groq: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

}