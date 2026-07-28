package com.jobboard.jobposting.dto;

import com.jobboard.jobposting.CareerLevel;
import com.jobboard.jobposting.EducationLevel;
import com.jobboard.jobposting.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobPostingCreateRequest(
        @NotBlank String pdfFileName,
        @NotBlank String companyName,
        @NotBlank String location,
        @NotNull CareerLevel careerLevel,
        @NotNull EducationLevel education,
        @NotNull EmploymentType employmentType,
        String conditionNote,
        @NotNull LocalDate applyStartDate,
        @NotNull LocalDate applyEndDate,
        String applyMethod,
        Integer salaryMin,
        Integer salaryMax,
        String salaryNote
) {
}
