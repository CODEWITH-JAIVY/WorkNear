package com.labourse.admin.repository;

import com.labourse.admin.entity.RefundRequest;
import com.labourse.admin.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    Page<RefundRequest> findByStatus(RefundStatus status, Pageable pageable);
    long countByStatus(RefundStatus status);
}
