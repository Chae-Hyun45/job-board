package com.jobboard.jobposting.dto;

public record PdfExtractionResult(
        String pdfFileName,
        String companyName,
        String location,
        String careerLevel,
        String education,
        String employmentType,
        String conditionNote,
        String applyStartDate,
        String applyEndDate,
        String applyMethod,
        Integer salaryMin,
        Integer salaryMax,
        String salaryNote
) {
}
