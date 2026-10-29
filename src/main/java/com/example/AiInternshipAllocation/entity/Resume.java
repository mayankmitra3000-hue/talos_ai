package com.example.AiInternshipAllocation.entity;

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
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "text")
    private String resumeText;

    @Column(columnDefinition = "bytea")
    private byte[] resumeFile;

    @Column(
            name = "is_default",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private Boolean isDefault = false;

}