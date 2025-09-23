package org.example.jpapaging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "JOBS", schema = "HR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @Column(name = "JOB_ID", length = 10, nullable = false)
    private String jobId;

    @Column(name = "JOB_TITLE", length = 35, nullable = false)
    private String jobTitle;

    @Column(name = "MIN_SALARY", precision = 6)
    private BigDecimal minSalary;

    @Column(name = "MAX_SALARY", precision = 6)
    private BigDecimal maxSalary;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<Employee> employees = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<JobHistory> jobHistories = new ArrayList<>();
}