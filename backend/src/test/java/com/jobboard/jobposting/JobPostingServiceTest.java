package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import com.jobboard.jobposting.dto.JobPostingCreateRequest;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import com.jobboard.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobPostingServiceTest {

    private FileStorageService fileStorageService;
    private PdfTextExtractor pdfTextExtractor;
    private OpenAiJobExtractionClient extractionClient;
    private JobPostingRepository jobPostingRepository;
    private UserRepository userRepository;
    private JobPostingService jobPostingService;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        pdfTextExtractor = mock(PdfTextExtractor.class);
        extractionClient = mock(OpenAiJobExtractionClient.class);
        jobPostingRepository = mock(JobPostingRepository.class);
        userRepository = mock(UserRepository.class);
        jobPostingService = new JobPostingService(
                jobPostingRepository, userRepository, fileStorageService, pdfTextExtractor, extractionClient);
    }

    @Test
    void PDF를_업로드하면_저장후_추출결과에_파일명을_채워_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "posting.pdf", "application/pdf", "content".getBytes());
        when(fileStorageService.store(file)).thenReturn("stored-uuid.pdf");
        when(fileStorageService.resolve("stored-uuid.pdf")).thenReturn(Path.of("stored-uuid.pdf"));
        when(pdfTextExtractor.extractText(any())).thenReturn("추출된 텍스트");
        when(extractionClient.extract("추출된 텍스트")).thenReturn(new PdfExtractionResult(
                null, "테스트회사", "서울", "NEW", "BACHELOR", "FULL_TIME", "비고",
                "2026-08-01", "2026-08-31", "이메일 접수", 3000, 3500, "협의가능"));

        PdfExtractionResult result = jobPostingService.extractFromPdf(file);

        assertThat(result.pdfFileName()).isEqualTo("stored-uuid.pdf");
        assertThat(result.companyName()).isEqualTo("테스트회사");
    }

    @Test
    void AI_추출이_실패하면_예외없이_파일명만_채운_결과를_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "posting.pdf", "application/pdf", "content".getBytes());
        when(fileStorageService.store(file)).thenReturn("stored-uuid.pdf");
        when(fileStorageService.resolve("stored-uuid.pdf")).thenReturn(Path.of("stored-uuid.pdf"));
        when(pdfTextExtractor.extractText(any())).thenReturn("추출된 텍스트");
        when(extractionClient.extract("추출된 텍스트"))
                .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "채용정보 추출에 실패했습니다. 직접 입력해주세요."));

        PdfExtractionResult result = jobPostingService.extractFromPdf(file);

        assertThat(result.pdfFileName()).isEqualTo("stored-uuid.pdf");
        assertThat(result.companyName()).isNull();
        assertThat(result.location()).isNull();
        assertThat(result.applyEndDate()).isNull();
    }

    @Test
    void 검토된_정보로_채용공고를_등록한다() {
        User admin = new User("admin@jobboard.com", "pw", "관리자", UserRole.ADMIN);
        when(userRepository.getReferenceById(1L)).thenReturn(admin);
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobPostingCreateRequest request = new JobPostingCreateRequest(
                "stored-uuid.pdf", "테스트회사", "서울", CareerLevel.NEW, EducationLevel.BACHELOR,
                EmploymentType.FULL_TIME, "비고", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                "이메일 접수", 3000, 3500, "협의가능");

        JobPosting posting = jobPostingService.create(request, 1L);

        assertThat(posting.getCompanyName()).isEqualTo("테스트회사");
        assertThat(posting.getStatus()).isEqualTo(JobPostingStatus.ACTIVE);
        assertThat(posting.getCreatedBy()).isEqualTo(admin);
    }

    @Test
    void 더미데이터_10개를_생성한다() {
        User admin = new User("admin@jobboard.com", "pw", "관리자", UserRole.ADMIN);
        when(userRepository.getReferenceById(1L)).thenReturn(admin);
        when(jobPostingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<JobPosting> dummies = jobPostingService.createDummyJobPostings(1L);

        assertThat(dummies).hasSize(10);
        assertThat(dummies).allSatisfy(posting -> {
            assertThat(posting.getCompanyName()).startsWith("[더미]");
            assertThat(posting.getCreatedBy()).isEqualTo(admin);
            assertThat(posting.getStatus()).isEqualTo(JobPostingStatus.ACTIVE);
        });
    }

    @Test
    void 더미데이터를_삭제한다() {
        jobPostingService.deleteDummyJobPostings();

        verify(jobPostingRepository).deleteByCompanyNameStartingWith("[더미] ");
    }

    @Nested
    @DataJpaTest
    class 검색과_자동마감 {

        @Autowired
        private JobPostingRepository repository;

        @Autowired
        private UserRepository users;

        @Autowired
        private TestEntityManager entityManager;

        private JobPostingService service;
        private User admin;

        @BeforeEach
        void setUp() {
            service = new JobPostingService(repository, users,
                    mock(FileStorageService.class), mock(PdfTextExtractor.class),
                    mock(OpenAiJobExtractionClient.class));
            admin = users.save(new User("admin@jobboard.com", "pw", "관리자", UserRole.ADMIN));
        }

        private JobPosting save(String companyName, String location, EmploymentType employmentType,
                               LocalDate applyEndDate, JobPostingStatus status) {
            JobPosting posting = new JobPosting();
            posting.setCompanyName(companyName);
            posting.setLocation(location);
            posting.setCareerLevel(CareerLevel.NEW);
            posting.setEducation(EducationLevel.BACHELOR);
            posting.setEmploymentType(employmentType);
            posting.setApplyStartDate(LocalDate.now().minusDays(10));
            posting.setApplyEndDate(applyEndDate);
            posting.setApplyMethod("이메일 접수");
            posting.setPdfFileName("sample.pdf");
            posting.setStatus(status);
            posting.setCreatedBy(admin);
            return repository.save(posting);
        }

        @Test
        void keyword로_검색하면_매칭되는_공고만_반환한다() {
            save("카카오", "서울", EmploymentType.FULL_TIME, LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE);
            save("네이버", "성남", EmploymentType.FULL_TIME, LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE);

            Page<JobPosting> page = service.search("카카오", null, null, PageRequest.of(0, 10));

            assertThat(page.getContent()).extracting(JobPosting::getCompanyName).containsExactly("카카오");
        }

        @Test
        void location과_employmentType_필터가_결과를_좁힌다() {
            save("카카오", "서울", EmploymentType.FULL_TIME, LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE);
            save("네이버", "성남", EmploymentType.FULL_TIME, LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE);
            save("토스", "서울", EmploymentType.INTERN, LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE);

            Page<JobPosting> byLocation = service.search(null, "서울", null, PageRequest.of(0, 10));
            assertThat(byLocation.getContent()).extracting(JobPosting::getCompanyName)
                    .containsExactlyInAnyOrder("카카오", "토스");

            Page<JobPosting> byType = service.search(null, "서울", EmploymentType.INTERN, PageRequest.of(0, 10));
            assertThat(byType.getContent()).extracting(JobPosting::getCompanyName).containsExactly("토스");
        }

        @Test
        void 검색결과에서_CLOSED_공고는_제외된다() {
            save("활성회사", "서울", EmploymentType.FULL_TIME, LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE);
            save("마감회사", "서울", EmploymentType.FULL_TIME, LocalDate.now().minusDays(1), JobPostingStatus.CLOSED);

            Page<JobPosting> page = service.search(null, null, null, PageRequest.of(0, 10));

            assertThat(page.getContent()).extracting(JobPosting::getCompanyName).containsExactly("활성회사");
        }

        @Test
        void closeExpired는_마감일이_지난_활성공고를_CLOSED로_저장한다() {
            Long expiredId = save("만료회사", "서울", EmploymentType.FULL_TIME,
                    LocalDate.now().minusDays(1), JobPostingStatus.ACTIVE).getId();
            Long activeId = save("진행회사", "서울", EmploymentType.FULL_TIME,
                    LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE).getId();

            service.closeExpired();
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(expiredId)).get()
                    .extracting(JobPosting::getStatus).isEqualTo(JobPostingStatus.CLOSED);
            assertThat(repository.findById(activeId)).get()
                    .extracting(JobPosting::getStatus).isEqualTo(JobPostingStatus.ACTIVE);
        }
    }
}
