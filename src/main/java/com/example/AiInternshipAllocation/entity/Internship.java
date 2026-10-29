package com.example.AiInternshipAllocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 2000)
    private String requiredSkills;

    private String experience;

    private String location;

    private Integer seats;

    private Integer allocatedCount = 0;

    private String postedBy;
}
