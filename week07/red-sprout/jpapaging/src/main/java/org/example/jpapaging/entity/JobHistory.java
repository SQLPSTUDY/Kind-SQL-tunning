package org.example.jpapaging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "JOB_HISTORY", schema = "HR")
@IdClass(JobHistoryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobHistory {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMPLOYEE_ID", nullable = false)
    private Employee employee;

    @Id
    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_ID", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPARTMENT_ID")
    private Department department;
}

// Composite Primary Key Class
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class JobHistoryId implements Serializable {
    private Long employee;
    private LocalDate startDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobHistoryId)) return false;
        JobHistoryId that = (JobHistoryId) o;
        return employee.equals(that.employee) && startDate.equals(that.startDate);
    }

    @Override
    public int hashCode() {
        return employee.hashCode() + startDate.hashCode();
    }
}