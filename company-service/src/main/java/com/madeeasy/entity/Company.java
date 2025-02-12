package com.madeeasy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_sequence_generator")
    @SequenceGenerator(
            name = "company_sequence_generator",
            sequenceName = "company_sequence",
            allocationSize = 1
    )
    private Long id;


    @Column(unique = true, nullable = false)
    private String emailId;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String domain;

    @Column(nullable = false)
    private Double autoApproveThreshold;
}
