package com.labourse.admin.service;

import com.labourse.admin.common.ApiException;
import com.labourse.admin.common.PageResponse;
import com.labourse.admin.dto.AdminDtos.DisputeDetail;
import com.labourse.admin.entity.Dispute;
import com.labourse.admin.entity.DisputeComment;
import com.labourse.admin.entity.DisputeStatus;
import com.labourse.admin.entity.Staff;
import com.labourse.admin.entity.StaffStatus;
import com.labourse.admin.repository.DisputeCommentRepository;
import com.labourse.admin.repository.DisputeRepository;
import com.labourse.admin.repository.StaffRepository;
import com.labourse.admin.security.Permission;
import com.labourse.admin.security.StaffPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository repository;
    private final DisputeCommentRepository commentRepository;
    private final StaffRepository staffRepository;
    private final AuditService audit;

    // ---------------- end-user side ----------------

    @Transactional
    public Dispute raise(Long jobId, Long userId, String userType, String reason) {
        if (repository.existsByJobIdAndRaisedByUserIdAndStatusIn(jobId, userId,
                List.of(DisputeStatus.OPEN, DisputeStatus.INVESTIGATING))) {
            throw ApiException.conflict("You already have an open dispute for this job");
        }
        Dispute d = new Dispute();
        d.setJobId(jobId);
        d.setRaisedByUserId(userId);
        d.setRaisedByType(userType);
        d.setReason(reason);
        return repository.save(d);
    }

    @Transactional(readOnly = true)
    public List<Dispute> mine(Long userId) {
        return repository.findByRaisedByUserIdOrderByCreatedAtDesc(userId);
    }

    // ---------------- staff side ----------------

    @Transactional(readOnly = true)
    public PageResponse<Dispute> search(StaffPrincipal staff, DisputeStatus status, int page, int size) {
        Specification<Dispute> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        String scope = staff.role().disputeScope();
        if (scope != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("raisedByType"), scope));
        }
        return PageResponse.of(repository.findAll(spec, PageResponse.pageable(page, size)));
    }

    @Transactional(readOnly = true)
    public DisputeDetail get(StaffPrincipal staff, Long id) {
        Dispute d = find(id);
        assertInScope(staff, d);
        return new DisputeDetail(d, commentRepository.findByDisputeIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public Dispute claim(StaffPrincipal staff, Long id) {
        Dispute d = find(id);
        assertInScope(staff, d);
        assertNotClosed(d);
        if (d.getAssignedToStaffId() != null && !d.getAssignedToStaffId().equals(staff.id())) {
            throw ApiException.conflict("Dispute is already assigned to another staff member");
        }
        d.setAssignedToStaffId(staff.id());
        d.setStatus(DisputeStatus.INVESTIGATING);
        repository.save(d);
        audit.record(staff, "DISPUTE_CLAIMED", "DISPUTE", id, null);
        return d;
    }

    @Transactional
    public Dispute assign(StaffPrincipal actor, Long id, Long targetStaffId) {
        Dispute d = find(id);
        assertNotClosed(d);

        Staff target = staffRepository.findById(targetStaffId)
                .orElseThrow(() -> ApiException.notFound("Staff member not found"));
        if (target.getStatus() != StaffStatus.ACTIVE) {
            throw ApiException.badRequest("Cannot assign to a disabled staff member");
        }
        if (!target.getRole().has(Permission.DISPUTE_RESOLVE)) {
            throw ApiException.badRequest("That staff member's role cannot resolve disputes");
        }
        String targetScope = target.getRole().disputeScope();
        if (targetScope != null && !targetScope.equals(d.getRaisedByType())) {
            throw ApiException.badRequest("That staff member's role cannot handle this type of dispute");
        }

        d.setAssignedToStaffId(targetStaffId);
        d.setStatus(DisputeStatus.INVESTIGATING);
        repository.save(d);
        audit.record(actor, "DISPUTE_ASSIGNED", "DISPUTE", id, "assignedTo=" + targetStaffId);
        return d;
    }

    @Transactional
    public DisputeComment addComment(StaffPrincipal staff, Long id, String text) {
        Dispute d = find(id);
        assertInScope(staff, d);
        DisputeComment c = new DisputeComment();
        c.setDisputeId(id);
        c.setStaffId(staff.id());
        c.setStaffName(staff.name());
        c.setBody(text.trim());
        commentRepository.save(c);
        audit.record(staff, "DISPUTE_COMMENTED", "DISPUTE", id, null);
        return c;
    }

    @Transactional
    public Dispute resolve(StaffPrincipal staff, Long id, String notes, boolean accepted) {
        Dispute d = find(id);
        assertInScope(staff, d);
        assertNotClosed(d);

        // A dispute claimed by someone else can only be closed by that person or by someone who can (re)assign.
        if (d.getAssignedToStaffId() != null
                && !d.getAssignedToStaffId().equals(staff.id())
                && !staff.role().has(Permission.DISPUTE_ASSIGN)) {
            throw ApiException.forbidden("This dispute is assigned to another staff member");
        }

        d.setStatus(accepted ? DisputeStatus.RESOLVED : DisputeStatus.REJECTED);
        d.setResolutionNotes(notes);
        d.setResolvedByStaffId(staff.id());
        d.setResolvedAt(LocalDateTime.now());
        repository.save(d);
        audit.record(staff, accepted ? "DISPUTE_RESOLVED" : "DISPUTE_REJECTED", "DISPUTE", id, notes);
        return d;
    }

    // ---------------- helpers ----------------

    private Dispute find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Dispute not found"));
    }

    private void assertInScope(StaffPrincipal staff, Dispute d) {
        String scope = staff.role().disputeScope();
        if (scope != null && !scope.equals(d.getRaisedByType())) {
            throw ApiException.forbidden("This dispute is outside your scope");
        }
    }

    private void assertNotClosed(Dispute d) {
        if (d.getStatus() == DisputeStatus.RESOLVED || d.getStatus() == DisputeStatus.REJECTED) {
            throw ApiException.conflict("Dispute is already closed");
        }
    }
}
