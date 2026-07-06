package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import com.example.AIResumeAnalyzer.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    void saveResume(String username, MultipartFile file, String resumeText);
    List<Resume> findByUser(User user);
    void deleteResume(Long id, String username);

}
