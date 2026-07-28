package com.jobboard.jobposting;

import com.jobboard.jobposting.dto.JobPostingCreateRequest;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import com.jobboard.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
