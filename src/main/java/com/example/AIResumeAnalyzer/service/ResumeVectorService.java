package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;

public interface ResumeVectorService {

    void indexResume(Resume resume);

    void deleteResumeEmbeddings(Long resumeId, String username);
}
