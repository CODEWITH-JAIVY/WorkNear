package com.labourse.job.repository;

import com.labourse.job.entity.JobInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobInterestRepository extends JpaRepository<JobInterest, Long> {
    List<JobInterest> findByJobIdOrderByCreatedAtAsc(Long jobId);
    Optional<JobInterest> findByJobIdAndLabourId(Long jobId, Long labourId);
    boolean existsByJobIdAndLabourId(Long jobId, Long labourId);
}
