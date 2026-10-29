package com.example.AiInternshipAllocation.service;

import java.util.List;
import java.util.Map;

public interface GroqService {

    String analyzeResume(String resumeText);

    String atsScoreGeneration(String resumeText);

    String compareResumesWithJob(String jobDescription, List<Map<String,Object>> matchingResumes);

}
