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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobPostingService {

    private static final String DUMMY_COMPANY_PREFIX = "[더미] ";
    private static final String[] DUMMY_LOCATIONS = {"서울", "부산", "대전", "인천", "광주", "대구", "성남", "수원", "판교", "제주"};

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
        PdfExtractionResult aiResult;
        try {
            aiResult = extractionClient.extract(text);
        } catch (ApiException e) {
            // AI 추출이 실패해도 저장된 PDF 파일명만 돌려주어 관리자가 수동으로 입력할 수 있게 한다.
            return new PdfExtractionResult(storedName, null, null, null, null, null, null,
                    null, null, null, null, null, null);
        }
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
        Specification<JobPosting> spec = Specification.where(JobPostingSpecifications.status(JobPostingStatus.ACTIVE));
        spec = andIfPresent(spec, JobPostingSpecifications.keyword(keyword));
        spec = andIfPresent(spec, JobPostingSpecifications.location(location));
        spec = andIfPresent(spec, JobPostingSpecifications.employmentType(employmentType));
        return jobPostingRepository.findAll(spec, pageable);
    }

    private Specification<JobPosting> andIfPresent(Specification<JobPosting> base, Specification<JobPosting> addition) {
        return addition == null ? base : base.and(addition);
    }

    public List<JobPosting> createDummyJobPostings(Long adminId) {
        User admin = userRepository.getReferenceById(adminId);
        CareerLevel[] careerLevels = CareerLevel.values();
        EducationLevel[] educationLevels = EducationLevel.values();
        EmploymentType[] employmentTypes = EmploymentType.values();

        List<JobPosting> dummies = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            JobPosting posting = new JobPosting();
            posting.setCompanyName(DUMMY_COMPANY_PREFIX + i + "번 회사");
            posting.setLocation(DUMMY_LOCATIONS[(i - 1) % DUMMY_LOCATIONS.length]);
            posting.setCareerLevel(careerLevels[(i - 1) % careerLevels.length]);
            posting.setEducation(educationLevels[(i - 1) % educationLevels.length]);
            posting.setEmploymentType(employmentTypes[(i - 1) % employmentTypes.length]);
            posting.setConditionNote("더미 데이터 " + i + "번");
            posting.setApplyStartDate(LocalDate.now());
            posting.setApplyEndDate(LocalDate.now().plusDays(30));
            posting.setApplyMethod("이메일 접수 (더미)");
            posting.setSalaryMin(3000 + i * 100);
            posting.setSalaryMax(3500 + i * 100);
            posting.setSalaryNote("협의가능");
            posting.setPdfFileName("dummy.pdf");
            posting.setCreatedBy(admin);
            dummies.add(posting);
        }
        return jobPostingRepository.saveAll(dummies);
    }

    @Transactional
    public void deleteDummyJobPostings() {
        jobPostingRepository.deleteByCompanyNameStartingWith(DUMMY_COMPANY_PREFIX);
    }

    public void closeExpired() {
        LocalDate today = LocalDate.now();
        List<JobPosting> expired = jobPostingRepository
                .findByStatusAndApplyEndDateBefore(JobPostingStatus.ACTIVE, today);
        expired.forEach(posting -> posting.setStatus(JobPostingStatus.CLOSED));
        jobPostingRepository.saveAll(expired);
    }
}
