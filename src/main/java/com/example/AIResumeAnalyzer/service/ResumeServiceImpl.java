package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import com.example.AIResumeAnalyzer.entity.User;
import com.example.AIResumeAnalyzer.repository.ResumeRepository;
import com.example.AIResumeAnalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    @Override
    public Resume saveResume(
            String username,
            MultipartFile file,
            String resumeText
    ) throws IOException {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        Resume resume = new Resume();

        resume.setFileName(
                file.getOriginalFilename()
        );

        resume.setUploadedAt(
                LocalDateTime.now()
        );

        resume.setResumeText(
                resumeText
        );

        resume.setResumeFile(
                file.getBytes()
        );

        resume.setUser(user);

        return resumeRepository.save(resume);
    }


    @Override
    public List<Resume> findByUser(User user) {
        return resumeRepository.findByUser(user);
    }

    @Override
    public void deleteResume(Long id, String username){
        Resume resume = resumeRepository
                .findByResumeIdAndUserUsername(id,username)
                .orElseThrow(()->
                        new AccessDeniedException("Resume not found or Access Denied !"));

        resumeRepository.delete(resume);
    }

    @Override
    public Resume getResumeByIdAndUsername(
            Long resumeId,
            String username
    ) {

        return resumeRepository
                .findByResumeIdAndUserUsername(
                        resumeId,
                        username
                )
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Resume not found or access denied"
                        )
                );
    }

}
