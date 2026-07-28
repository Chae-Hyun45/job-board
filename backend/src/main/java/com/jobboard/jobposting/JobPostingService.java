package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import com.jobboard.jobposting.dto.JobPostingCreateRequest;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final OpenAiJobExtractionClient extractionClient;

    public JobPostingService(JobPostingRepository jobPostingRepository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService,
                              PdfTextExtractor pdfTextExtractor,
                              OpenAiJobExtractionClient extractionClient) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.extractionClient = extractionClient;
    }

    public PdfExtractionResult extractFromPdf(MultipartFile file) {
        String storedName = fileStorageService.store(file);
        String text = pdfTextExtractor.extractText(fileStorageService.resolve(storedName));
        PdfExtractionResult aiResult = extractionClient.extract(text);
        return new PdfExtractionResult(
                storedName, aiResult.companyName(), aiResult.location(), aiResult.careerLevel(),
                aiResult.education(), aiResult.employmentType(), aiResult.conditionNote(),
                aiResult.applyStartDate(), aiResult.applyEndDate(), aiResult.applyMethod(),
                aiResult.salaryMin(), aiResult.salaryMax(), aiResult.salaryNote());
    }

    public JobPosting create(JobPostingCreateRequest request, Long adminId) {
        User admin = userRepository.getReferenceById(adminId);

        JobPosting posting = new JobPosting();
        posting.setCompanyName(request.companyName());
        posting.setLocation(request.location());
        posting.setCareerLevel(request.careerLevel());
        posting.setEducation(request.education());
        posting.setEmploymentType(request.employmentType());
        posting.setConditionNote(request.conditionNote());
        posting.setApplyStartDate(request.applyStartDate());
        posting.setApplyEndDate(request.applyEndDate());
        posting.setApplyMethod(request.applyMethod());
        posting.setSalaryMin(request.salaryMin());
        posting.setSalaryMax(request.salaryMax());
        posting.setSalaryNote(request.salaryNote());
        posting.setPdfFileName(request.pdfFileName());
        posting.setCreatedBy(admin);

        return jobPostingRepository.save(posting);
    }

    public JobPosting update(Long id, JobPostingCreateRequest request) {
        JobPosting posting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다."));
        posting.setCompanyName(request.companyName());
        posting.setLocation(request.location());
        posting.setCareerLevel(request.careerLevel());
        posting.setEducation(request.education());
        posting.setEmploymentType(request.employmentType());
        posting.setConditionNote(request.conditionNote());
        posting.setApplyStartDate(request.applyStartDate());
        posting.setApplyEndDate(request.applyEndDate());
        posting.setApplyMethod(request.applyMethod());
        posting.setSalaryMin(request.salaryMin());
        posting.setSalaryMax(request.salaryMax());
        posting.setSalaryNote(request.salaryNote());
        posting.setUpdatedAt(LocalDateTime.now());
        return jobPostingRepository.save(posting);
    }

    public void delete(Long id) {
        if (!jobPostingRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다.");
        }
        jobPostingRepository.deleteById(id);
    }

    public List<JobPosting> findAllForAdmin() {
        return jobPostingRepository.findAll();
    }

    public JobPosting getById(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다."));
    }

    public Page<JobPosting> search(String keyword, String location, EmploymentType employmentType, Pageable pageable) {
        Specification<JobPosting> spec = Specification
                .where(JobPostingSpecifications.status(JobPostingStatus.ACTIVE))
                .and(JobPostingSpecifications.keyword(keyword))
                .and(JobPostingSpecifications.location(location))
                .and(JobPostingSpecifications.employmentType(employmentType));
        return jobPostingRepository.findAll(spec, pageable);
    }

    public void closeExpired() {
        LocalDate today = LocalDate.now();
        List<JobPosting> expired = jobPostingRepository
                .findByStatusAndApplyEndDateBefore(JobPostingStatus.ACTIVE, today);
        expired.forEach(posting -> posting.setStatus(JobPostingStatus.CLOSED));
        jobPostingRepository.saveAll(expired);
    }
}
