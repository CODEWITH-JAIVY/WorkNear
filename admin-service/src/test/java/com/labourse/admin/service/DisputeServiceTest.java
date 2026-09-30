package com.labourse.admin.service;

import com.labourse.admin.entity.Dispute;
import com.labourse.admin.repository.DisputeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock DisputeRepository repository;
    @InjectMocks DisputeService disputeService;

    @Test
    void raise_createsDisputeWithOpenStatus() {
        when(repository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute d = disputeService.raise(10L, 1L, "Labour never showed up");

        assertThat(d.getStatus()).isEqualTo("OPEN");
        assertThat(d.getJobId()).isEqualTo(10L);
    }

    @Test
    void resolve_setsResolvedStatus_andTimestamp_whenAccepted() {
        Dispute existing = new Dispute();
        existing.setId(5L);
        existing.setStatus("OPEN");

        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute resolved = disputeService.resolve(5L, "Refund issued", true);

        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");
        assertThat(resolved.getResolvedAt()).isNotNull();
    }

    @Test
    void resolve_setsRejectedStatus_whenNotAccepted() {
        Dispute existing = new Dispute();
        existing.setId(6L);
        existing.setStatus("OPEN");

        when(repository.findById(6L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute resolved = disputeService.resolve(6L, "No evidence found", false);

        assertThat(resolved.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    void resolve_throws_whenDisputeNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disputeService.resolve(99L, "notes", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
