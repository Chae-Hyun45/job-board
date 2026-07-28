package com.jobboard.jobposting;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobPostingExpirationScheduler {

    private final JobPostingService jobPostingService;

    public JobPostingExpirationScheduler(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void closeExpiredPostings() {
        jobPostingService.closeExpired();
    }
}
