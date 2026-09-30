package com.labourse.job.repository;

import com.labourse.job.entity.JobPost;
import com.labourse.job.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {
    List<JobPost> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<JobPost> findByStatus(JobStatus status);
}
