package com.example.AiInternshipAllocation.service;

import com.example.AiInternshipAllocation.entity.Resume;
import com.example.AiInternshipAllocation.entity.User;
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

    Resume setDefaultResume(
            Long resumeId,
            String username
    );

    Resume getDefaultResume(String username);

}
