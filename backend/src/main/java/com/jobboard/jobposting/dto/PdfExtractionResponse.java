package com.jobboard.jobposting.dto;

public record PdfExtractionResponse(
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
    public static PdfExtractionResponse from(PdfExtractionResult result) {
        return new PdfExtractionResponse(
                result.pdfFileName(), result.companyName(), result.location(), result.careerLevel(),
                result.education(), result.employmentType(), result.conditionNote(), result.applyStartDate(),
                result.applyEndDate(), result.applyMethod(), result.salaryMin(), result.salaryMax(), result.salaryNote());
    }
}
