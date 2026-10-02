package com.labourse.admin.repository;

import com.labourse.admin.entity.DisputeComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeCommentRepository extends JpaRepository<DisputeComment, Long> {
    List<DisputeComment> findByDisputeIdOrderByCreatedAtAsc(Long disputeId);
}
