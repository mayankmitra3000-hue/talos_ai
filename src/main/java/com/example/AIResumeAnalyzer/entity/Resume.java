package com.example.AIResumeAnalyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resumeId;

    private String fileName;

    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name ="id")
    private User user;

    @Lob
    @Column(columnDefinition="LONGTEXT")
    private String resumeText;

}