package com.example.AiInternshipAllocation.repository;

import com.example.AiInternshipAllocation.entity.InternshipAllocation;
import com.example.AiInternshipAllocation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipAllocationRepository extends JpaRepository<InternshipAllocation, Long> {
    List<InternshipAllocation> findByStudent(User student);
    boolean existsByStudent(User student);
    boolean existsByStudentAndInternshipId(User student, Long internshipId);
}
