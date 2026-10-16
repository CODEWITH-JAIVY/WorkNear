package com.labourse.admin.repository;

import com.labourse.admin.entity.Dispute;
import com.labourse.admin.entity.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long>, JpaSpecificationExecutor<Dispute> {
    long countByStatus(DisputeStatus status);
    boolean existsByJobIdAndRaisedByUserIdAndStatusIn(Long jobId, Long raisedByUserId, Collection<DisputeStatus> statuses);
    List<Dispute> findByRaisedByUserIdOrderByCreatedAtDesc(Long raisedByUserId);
}
