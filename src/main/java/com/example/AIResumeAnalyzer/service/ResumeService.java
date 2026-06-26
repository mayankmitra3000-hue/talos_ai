package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    void saveResume(String username, MultipartFile file, String resumeText);

}
