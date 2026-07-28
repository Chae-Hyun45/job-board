package com.jobboard.jobposting;

import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import com.jobboard.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobPostingRepositoryTest {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private UserRepository userRepository;

    private JobPosting newPosting(LocalDate endDate, JobPostingStatus status) {
        User admin = userRepository.save(new User("admin" + Math.random() + "@jobboard.com", "pw", "관리자", UserRole.ADMIN));
        JobPosting posting = new JobPosting();
        posting.setCompanyName("테스트회사");
        posting.setLocation("서울");
        posting.setCareerLevel(CareerLevel.NEW);
        posting.setEducation(EducationLevel.BACHELOR);
        posting.setEmploymentType(EmploymentType.FULL_TIME);
        posting.setConditionNote("우대사항 없음");
        posting.setApplyStartDate(LocalDate.now().minusDays(10));
        posting.setApplyEndDate(endDate);
        posting.setApplyMethod("이메일 접수");
        posting.setSalaryMin(3000);
        posting.setSalaryMax(3500);
        posting.setSalaryNote("협의가능");
        posting.setPdfFileName("sample.pdf");
        posting.setStatus(status);
        posting.setCreatedBy(admin);
        return posting;
    }

    @Test
    void 마감일이_지난_활성_공고를_조회한다() {
        jobPostingRepository.save(newPosting(LocalDate.now().minusDays(1), JobPostingStatus.ACTIVE));
        jobPostingRepository.save(newPosting(LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE));
        jobPostingRepository.save(newPosting(LocalDate.now().minusDays(1), JobPostingStatus.CLOSED));

        List<JobPosting> expired = jobPostingRepository
                .findByStatusAndApplyEndDateBefore(JobPostingStatus.ACTIVE, LocalDate.now());

        assertThat(expired).hasSize(1);
    }
}
