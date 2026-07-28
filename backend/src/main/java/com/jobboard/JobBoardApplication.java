package com.jobboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobBoardApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobBoardApplication.class, args);
    }
}
