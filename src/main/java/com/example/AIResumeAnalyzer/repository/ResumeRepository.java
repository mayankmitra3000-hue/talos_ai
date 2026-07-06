package com.example.AIResumeAnalyzer.repository;
import com.example.AIResumeAnalyzer.entity.Resume;
import com.example.AIResumeAnalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume,Long> {
    List<Resume> findByUser(User user);
    Optional<Resume> findByResumeIdAndUserUsername(Long id, String username);
}
