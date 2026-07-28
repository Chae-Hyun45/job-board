package com.jobboard.jobposting;

import com.jobboard.common.SessionKeys;
import com.jobboard.jobposting.dto.JobPostingCreateRequest;
import com.jobboard.jobposting.dto.JobPostingResponse;
import com.jobboard.jobposting.dto.PdfExtractionResponse;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/job-postings")
public class AdminJobPostingController {

    private final JobPostingService jobPostingService;

    public AdminJobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PdfExtractionResponse extract(@RequestParam("file") MultipartFile file) {
        PdfExtractionResult result = jobPostingService.extractFromPdf(file);
        return PdfExtractionResponse.from(result);
    }

    @PostMapping
    public JobPostingResponse create(@Valid @RequestBody JobPostingCreateRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getSession().getAttribute(SessionKeys.USER_ID);
        JobPosting posting = jobPostingService.create(request, adminId);
        return JobPostingResponse.from(posting);
    }

    @GetMapping
    public List<JobPostingResponse> list() {
        return jobPostingService.findAllForAdmin().stream().map(JobPostingResponse::from).toList();
    }

    @PutMapping("/{id}")
    public JobPostingResponse update(@PathVariable Long id, @Valid @RequestBody JobPostingCreateRequest request) {
        return JobPostingResponse.from(jobPostingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
