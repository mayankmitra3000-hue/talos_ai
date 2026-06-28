package com.example.AIResumeAnalyzer.service;

import com.example.AIResumeAnalyzer.entity.Resume;
import com.example.AIResumeAnalyzer.entity.User;
import com.example.AIResumeAnalyzer.repository.ResumeRepository;
import com.example.AIResumeAnalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    @Override
    public void saveResume(String username, MultipartFile file, String resumeText){

        User user = userRepository.findByUsername(username);

        if(user==null){
            throw new IllegalArgumentException("User not found !");
        }

        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setUploadedAt(LocalDateTime.now());
        resume.setResumeText(resumeText);
        resume.setUser(user);

        resumeRepository.save(resume);
    }

    @Override
    public List<Resume> findByUser(User user) {
        return resumeRepository.findByUser(user);
    }


}
