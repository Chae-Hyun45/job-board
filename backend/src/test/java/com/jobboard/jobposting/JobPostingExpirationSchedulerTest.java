package com.jobboard.jobposting;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JobPostingExpirationSchedulerTest {

    @Test
    void 스케줄러_실행시_서비스의_마감처리를_호출한다() {
        JobPostingService jobPostingService = mock(JobPostingService.class);
        JobPostingExpirationScheduler scheduler = new JobPostingExpirationScheduler(jobPostingService);

        scheduler.closeExpiredPostings();

        verify(jobPostingService).closeExpired();
    }
}
