package com.jobboard.jobposting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {
    List<JobPosting> findByStatusAndApplyEndDateBefore(JobPostingStatus status, LocalDate date);

    long deleteByCompanyNameStartingWith(String prefix);
}
