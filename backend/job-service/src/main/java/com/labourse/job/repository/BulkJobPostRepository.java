package com.labourse.job.repository;

import com.labourse.job.entity.BulkJobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BulkJobPostRepository extends JpaRepository<BulkJobPost, Long> {
    List<BulkJobPost> findByContractorIdOrderByCreatedAtDesc(Long contractorId);
}
