package com.example.AiInternshipAllocation.service;

import com.example.AiInternshipAllocation.entity.Resume;

public interface ResumeVectorService {

    void indexResume(Resume resume);

    void deleteResumeEmbeddings(Long resumeId, String username);
}
