package com.labourse.admin.service;

import com.labourse.admin.common.ApiException;
import com.labourse.admin.entity.Dispute;
import com.labourse.admin.entity.DisputeStatus;
import com.labourse.admin.repository.DisputeCommentRepository;
import com.labourse.admin.repository.DisputeRepository;
import com.labourse.admin.repository.StaffRepository;
import com.labourse.admin.security.StaffPrincipal;
import com.labourse.admin.security.StaffRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock DisputeRepository repository;
    @Mock DisputeCommentRepository commentRepository;
    @Mock StaffRepository staffRepository;
    @Mock AuditService audit;
    @InjectMocks DisputeService service;

    private final StaffPrincipal admin = new StaffPrincipal(7L, "admin@x.com", "Admin", StaffRole.ADMIN);

    private Dispute dispute(Long id, String raisedByType, DisputeStatus status) {
        Dispute d = new Dispute();
        d.setId(id);
        d.setRaisedByType(raisedByType);
        d.setStatus(status);
        return d;
    }

    @Test
    void raise_createsOpenDispute() {
        when(repository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute d = service.raise(10L, 1L, "CUSTOMER", "Labour never showed up");

        assertThat(d.getStatus()).isEqualTo(DisputeStatus.OPEN);
        assertThat(d.getJobId()).isEqualTo(10L);
        assertThat(d.getRaisedByType()).isEqualTo("CUSTOMER");
    }

    @Test
    void raise_rejectsDuplicateOpenDisputeForSameJob() {
        when(repository.existsByJobIdAndRaisedByUserIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.raise(10L, 1L, "CUSTOMER", "again"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void resolve_setsResolved_andRecordsStaffAndTimestamp_whenAccepted() {
        when(repository.findById(5L)).thenReturn(Optional.of(dispute(5L, "CUSTOMER", DisputeStatus.OPEN)));
        when(repository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute resolved = service.resolve(admin, 5L, "Refund issued", true);

        assertThat(resolved.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getResolvedByStaffId()).isEqualTo(7L);
    }

    @Test
    void resolve_setsRejected_whenNotAccepted() {
        when(repository.findById(6L)).thenReturn(Optional.of(dispute(6L, "LABOUR", DisputeStatus.OPEN)));
        when(repository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute resolved = service.resolve(admin, 6L, "No evidence found", false);

        assertThat(resolved.getStatus()).isEqualTo(DisputeStatus.REJECTED);
    }

    @Test
    void resolve_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(admin, 99L, "notes", true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void resolve_rejectsAlreadyClosedDispute() {
        when(repository.findById(8L)).thenReturn(Optional.of(dispute(8L, "CUSTOMER", DisputeStatus.RESOLVED)));

        assertThatThrownBy(() -> service.resolve(admin, 8L, "again", true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void resolve_blocksCustomerExecutiveFromLabourDispute() {
        StaffPrincipal customerExec =
                new StaffPrincipal(11L, "ce@x.com", "CE", StaffRole.CUSTOMER_EXECUTIVE);
        when(repository.findById(9L)).thenReturn(Optional.of(dispute(9L, "LABOUR", DisputeStatus.OPEN)));

        assertThatThrownBy(() -> service.resolve(customerExec, 9L, "notes", true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("scope");
    }

    @Test
    void resolve_blocksExecutiveWhenAssignedToSomeoneElse() {
        StaffPrincipal customerExec =
                new StaffPrincipal(11L, "ce@x.com", "CE", StaffRole.CUSTOMER_EXECUTIVE);
        Dispute d = dispute(12L, "CUSTOMER", DisputeStatus.INVESTIGATING);
        d.setAssignedToStaffId(99L);
        when(repository.findById(12L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.resolve(customerExec, 12L, "notes", true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("assigned");
    }
}
