package com.labourse.admin.service;

import com.labourse.admin.common.ApiException;
import com.labourse.admin.dto.AdminDtos.RefundCreateRequest;
import com.labourse.admin.entity.RefundRequest;
import com.labourse.admin.entity.RefundStatus;
import com.labourse.admin.repository.DisputeRepository;
import com.labourse.admin.repository.RefundRequestRepository;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.security.StaffRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock RefundRequestRepository repository;
    @Mock DisputeRepository disputeRepository;
    @Mock AuditService audit;
    @InjectMocks RefundService service;

    private final StaffPrincipal maker = new StaffPrincipal(5L, "maker@x.com", "Maker", StaffRole.FINANCE);
    private final StaffPrincipal checker = new StaffPrincipal(6L, "checker@x.com", "Checker", StaffRole.FINANCE);

    private RefundRequest pending(Long id, Long requestedBy) {
        RefundRequest r = new RefundRequest();
        r.setId(id);
        r.setRequestedByStaffId(requestedBy);
        r.setStatus(RefundStatus.PENDING);
        r.setAmount(new BigDecimal("500.00"));
        return r;
    }

    @Test
    void approve_byTheStaffMemberWhoRequestedIt_isForbidden() {
        when(repository.findById(1L)).thenReturn(Optional.of(pending(1L, 5L)));

        assertThatThrownBy(() -> service.approve(maker, 1L, "ok"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Maker-checker");
    }

    @Test
    void approve_byDifferentStaffMember_succeeds() {
        when(repository.findById(2L)).thenReturn(Optional.of(pending(2L, 5L)));
        when(repository.save(any(RefundRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundRequest r = service.approve(checker, 2L, "verified");

        assertThat(r.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(r.getDecidedByStaffId()).isEqualTo(6L);
        assertThat(r.getDecidedAt()).isNotNull();
    }

    @Test
    void approve_alreadyDecidedRefund_isConflict() {
        RefundRequest r = pending(3L, 5L);
        r.setStatus(RefundStatus.REJECTED);
        when(repository.findById(3L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.approve(checker, 3L, "late"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already");
    }

    @Test
    void create_withUnknownDispute_isNotFound() {
        when(disputeRepository.existsById(99L)).thenReturn(false);

        RefundCreateRequest req = new RefundCreateRequest(1L, 99L, new BigDecimal("100"), "reason");

        assertThatThrownBy(() -> service.create(maker, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }
}
