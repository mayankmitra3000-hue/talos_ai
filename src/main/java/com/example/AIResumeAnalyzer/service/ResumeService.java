package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import com.example.AIResumeAnalyzer.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ResumeService {

    Resume saveResume(String username, MultipartFile file, String resumeText)throws IOException;

    List<Resume> findByUser(User user);

    void deleteResume(Long id, String username);

    Resume getResumeByIdAndUsername(
            Long resumeId,
            String username
    );

}
