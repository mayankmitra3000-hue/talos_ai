package com.example.AiInternshipAllocation.service;

import java.util.List;
import java.util.Map;

public interface BestResumeService {

    List<Map<String,Object>> findBestResumes(String username, String jobDescription);
}
