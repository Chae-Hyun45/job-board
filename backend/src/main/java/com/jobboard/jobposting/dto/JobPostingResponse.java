package com.jobboard.jobposting.dto;

import com.jobboard.jobposting.CareerLevel;
import com.jobboard.jobposting.EducationLevel;
import com.jobboard.jobposting.EmploymentType;
import com.jobboard.jobposting.JobPosting;
import com.jobboard.jobposting.JobPostingStatus;

import java.time.LocalDate;

public record JobPostingResponse(
        Long id,
        String companyName,
        String location,
        CareerLevel careerLevel,
        EducationLevel education,
        EmploymentType employmentType,
        String conditionNote,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        String applyMethod,
        Integer salaryMin,
        Integer salaryMax,
        String salaryNote,
        JobPostingStatus status
) {
    public static JobPostingResponse from(JobPosting posting) {
        return new JobPostingResponse(
                posting.getId(), posting.getCompanyName(), posting.getLocation(), posting.getCareerLevel(),
                posting.getEducation(), posting.getEmploymentType(), posting.getConditionNote(),
                posting.getApplyStartDate(), posting.getApplyEndDate(), posting.getApplyMethod(),
                posting.getSalaryMin(), posting.getSalaryMax(), posting.getSalaryNote(), posting.getStatus());
    }
}
