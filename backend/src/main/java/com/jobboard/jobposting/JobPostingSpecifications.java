package com.jobboard.jobposting;

import org.springframework.data.jpa.domain.Specification;

public final class JobPostingSpecifications {

    private JobPostingSpecifications() {
    }

    public static Specification<JobPosting> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("companyName")), pattern);
    }

    public static Specification<JobPosting> location(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String pattern = "%" + location.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("location")), pattern);
    }

    public static Specification<JobPosting> employmentType(EmploymentType employmentType) {
        if (employmentType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("employmentType"), employmentType);
    }

    public static Specification<JobPosting> status(JobPostingStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
