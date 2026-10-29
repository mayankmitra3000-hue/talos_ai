package com.example.AiInternshipAllocation.service;

import com.example.AiInternshipAllocation.entity.Resume;
import com.example.AiInternshipAllocation.entity.User;
import com.example.AiInternshipAllocation.repository.ResumeRepository;
import com.example.AiInternshipAllocation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        /*
         * The first resume uploaded by a student automatically
         * becomes their default resume. Later uploads do not
         * replace an existing default.
         */
        resume.setIsDefault(
                !resumeRepository.existsByUserUsernameAndIsDefaultTrue(
                        username
                )
        );

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

        boolean wasDefault = Boolean.TRUE.equals(resume.getIsDefault());

        resumeRepository.delete(resume);

        /*
         * A student must always keep one default resume if any
         * resumes remain. Promote the most recently uploaded one.
         */
        if (wasDefault) {
            List<Resume> remaining =
                    resumeRepository.findByUserUsernameOrderByUploadedAtDesc(
                            username
                    );

            if (!remaining.isEmpty()) {
                Resume replacement = remaining.get(0);
                replacement.setIsDefault(true);
                resumeRepository.save(replacement);
            }
        }
    }

    @Override
    @Transactional
    public Resume setDefaultResume(
            Long resumeId,
            String username
    ) {
        Resume selected =
                getResumeByIdAndUsername(resumeId, username);

        List<Resume> resumes =
                resumeRepository.findByUserUsernameOrderByUploadedAtDesc(
                        username
                );

        for (Resume resume : resumes) {
            boolean isDefault =
                    resume.getResumeId().equals(resumeId);

            if (!Boolean.valueOf(isDefault)
                    .equals(resume.getIsDefault())) {

                resume.setIsDefault(isDefault);
                resumeRepository.save(resume);
            }
        }

        return selected;
    }

    @Override
    @Transactional
    public Resume getDefaultResume(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        Optional<Resume> defaultResume =
                resumeRepository
                        .findFirstByUserUsernameAndIsDefaultTrue(
                                username
                        );

        if (defaultResume.isPresent()) {
            return defaultResume.get();
        }

        /*
         * Backward compatibility for resumes uploaded before
         * the default-resume feature existed.
         */
        List<Resume> resumes =
                resumeRepository.findByUserUsernameOrderByUploadedAtDesc(
                        username
                );

        if (resumes.isEmpty()) {
            return null;
        }

        Resume latest = resumes.get(0);
        latest.setIsDefault(true);
        return resumeRepository.save(latest);
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