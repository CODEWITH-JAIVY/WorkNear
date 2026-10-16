package com.labourse.admin.service;

import com.labourse.admin.common.ApiException;
import com.labourse.admin.common.PageResponse;
import com.labourse.admin.dto.AdminDtos.RefundCreateRequest;
import com.labourse.admin.entity.RefundRequest;
import com.labourse.admin.entity.RefundStatus;
import com.labourse.admin.repository.DisputeRepository;
import com.labourse.admin.repository.RefundRequestRepository;
import com.labourse.admin.security.StaffPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Maker-checker workflow. Approving only records the decision here;
 * moving the money is payment-service's job (see README, "Next integrations").
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository repository;
    private final DisputeRepository disputeRepository;
    private final AuditService audit;

    @Transactional
    public RefundRequest create(StaffPrincipal staff, RefundCreateRequest req) {
        if (req.disputeId() != null && !disputeRepository.existsById(req.disputeId())) {
            throw ApiException.notFound("Dispute not found");
        }
        RefundRequest r = new RefundRequest();
        r.setJobId(req.jobId());
        r.setDisputeId(req.disputeId());
        r.setAmount(req.amount());
        r.setReason(req.reason());
        r.setRequestedByStaffId(staff.id());
        repository.save(r);
        audit.record(staff, "REFUND_REQUESTED", "REFUND", r.getId(), "jobId=" + req.jobId() + ", amount=" + req.amount());
        return r;
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundRequest> list(RefundStatus status, int page, int size) {
        Pageable pageable = PageResponse.pageable(page, size);
        return PageResponse.of(status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable));
    }

    @Transactional
    public RefundRequest approve(StaffPrincipal staff, Long id, String notes) {
        RefundRequest r = findPending(id);
        if (staff.id().equals(r.getRequestedByStaffId())) {
            throw ApiException.forbidden("Maker-checker: you cannot approve a refund you requested yourself");
        }
        return decide(staff, r, RefundStatus.APPROVED, "REFUND_APPROVED", notes);
    }

    @Transactional
    public RefundRequest reject(StaffPrincipal staff, Long id, String notes) {
        RefundRequest r = findPending(id);
        return decide(staff, r, RefundStatus.REJECTED, "REFUND_REJECTED", notes);
    }

    private RefundRequest decide(StaffPrincipal staff, RefundRequest r, RefundStatus status, String action, String notes) {
        r.setStatus(status);
        r.setDecidedByStaffId(staff.id());
        r.setDecisionNotes(notes);
        r.setDecidedAt(LocalDateTime.now());
        repository.save(r);
        audit.record(staff, action, "REFUND", r.getId(), notes);
        return r;
    }

    private RefundRequest findPending(Long id) {
        RefundRequest r = repository.findById(id).orElseThrow(() -> ApiException.notFound("Refund request not found"));
        if (r.getStatus() != RefundStatus.PENDING) {
            throw ApiException.conflict("Refund request is already " + r.getStatus());
        }
        return r;
    }
}
