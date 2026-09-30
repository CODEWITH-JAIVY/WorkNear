package com.labourse.admin.service;

import com.labourse.admin.entity.Dispute;
import com.labourse.admin.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository repository;

    public Dispute raise(Long jobId, Long userId, String reason) {
        Dispute d = new Dispute();
        d.setJobId(jobId);
        d.setRaisedByUserId(userId);
        d.setReason(reason);
        return repository.save(d);
    }

    public List<Dispute> openDisputes() {
        return repository.findByStatus("OPEN");
    }

    public Dispute resolve(Long disputeId, String notes, boolean accepted) {
        Dispute d = repository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        d.setStatus(accepted ? "RESOLVED" : "REJECTED");
        d.setResolutionNotes(notes);
        d.setResolvedAt(LocalDateTime.now());
        return repository.save(d);
    }
}
