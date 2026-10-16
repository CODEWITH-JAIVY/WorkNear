package com.labourse.job.repository;

import com.labourse.job.entity.BulkJobInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BulkJobInterestRepository extends JpaRepository<BulkJobInterest, Long> {
    List<BulkJobInterest> findByBulkJobIdOrderByCreatedAtAsc(Long bulkJobId);
    boolean existsByBulkJobIdAndLabourId(Long bulkJobId, Long labourId);
}
