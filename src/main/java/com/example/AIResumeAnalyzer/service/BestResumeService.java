package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;

import java.util.List;
import java.util.Map;

public interface BestResumeService {

    List<Map<String,Object>> findBestResumes(String username, String jobDescription);
}
