package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import com.example.AIResumeAnalyzer.entity.User;
import com.example.AIResumeAnalyzer.repository.ResumeRepository;
import com.example.AIResumeAnalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVectorService resumeVectorService;

    @Override
    public Resume saveResume(
            String username,
            MultipartFile file,
            String resumeText
    ) throws IOException {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file cannot be empty");
        }

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Resume resume = new Resume();

        resume.setFileName(file.getOriginalFilename());
        resume.setUploadedAt(LocalDateTime.now());
        resume.setResumeText(resumeText);
        resume.setResumeFile(file.getBytes());
        resume.setUser(user);

        return resumeRepository.save(resume);
    }

    @Override
    public List<Resume> findByUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return resumeRepository.findByUser(user);
    }

    @Override
    public void deleteResume(
            Long resumeId,
            String username
    ) {

        if (resumeId == null) {
            throw new IllegalArgumentException("Resume ID cannot be null");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        Resume resume = resumeRepository
                .findByResumeIdAndUserUsername(
                        resumeId,
                        username
                )
                .orElseThrow(() ->
                        new SecurityException(
                                "Resume not found or access denied"
                        )
                );

        /*
         * Delete Qdrant embeddings first.
         * This prevents deleted resumes from appearing
         * in vector-search results.
         */
        resumeVectorService.deleteResumeEmbeddings(
                resume.getResumeId(),
                username
        );

        resumeRepository.delete(resume);
    }

    @Override
    public Resume getResumeByIdAndUsername(
            Long resumeId,
            String username
    ) {

        if (resumeId == null) {
            throw new IllegalArgumentException(
                    "Resume ID cannot be null"
            );
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        return resumeRepository
                .findByResumeIdAndUserUsername(
                        resumeId,
                        username
                )
                .orElseThrow(() ->
                        new SecurityException(
                                "Resume not found or access denied"
                        )
                );
    }
}