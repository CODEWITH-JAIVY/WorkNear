package com.labourse.kyc.service;

import com.labourse.kyc.client.LabourServiceClient;
import com.labourse.kyc.dto.ReviewDocumentRequest;
import com.labourse.kyc.dto.SubmitDocumentRequest;
import com.labourse.kyc.entity.DocType;
import com.labourse.kyc.entity.KycDocument;
import com.labourse.kyc.entity.KycStatus;
import com.labourse.kyc.event.KycEventPublisher;
import com.labourse.kyc.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository repository;
    private final LabourServiceClient labourServiceClient;
    private final KycEventPublisher eventPublisher;

    // Minimum doc set required before a labour can go live on the platform
    private static final Set<DocType> REQUIRED_DOCS = Set.of(DocType.AADHAAR, DocType.PAN, DocType.SELFIE);

    @Transactional
    public KycDocument submit(Long labourId, SubmitDocumentRequest req) {
        KycDocument doc = repository.findByLabourId(labourId).stream()
                .filter(d -> d.getDocType() == req.getDocType())
                .findFirst()
                .orElseGet(KycDocument::new);

        doc.setLabourId(labourId);
        doc.setDocType(req.getDocType());
        doc.setDocumentUrl(req.getDocumentUrl());
        doc.setStatus(KycStatus.PENDING); // re-submission resets to pending, even if previously reviewed
        doc.setReviewNotes(null);
        return repository.save(doc);
    }

    public List<KycDocument> pendingReview() {
        return repository.findByStatus(KycStatus.PENDING);
    }

    public List<KycDocument> forLabour(Long labourId) {
        return repository.findByLabourId(labourId);
    }

    @Transactional
    public KycDocument review(Long docId, Long adminId, ReviewDocumentRequest req) {
        KycDocument doc = repository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        doc.setStatus(req.isApproved() ? KycStatus.VERIFIED : KycStatus.REJECTED);
        doc.setReviewNotes(req.getNotes());
        doc.setReviewedByAdminId(adminId);
        doc.setReviewedAt(LocalDateTime.now());
        doc = repository.save(doc);

        if (req.isApproved()) {
            checkAndPromoteIfFullyVerified(doc.getLabourId());
        }
        return doc;
    }

    private void checkAndPromoteIfFullyVerified(Long labourId) {
        List<KycDocument> docs = repository.findByLabourId(labourId);
        Set<DocType> verifiedTypes = docs.stream()
                .filter(d -> d.getStatus() == KycStatus.VERIFIED)
                .map(KycDocument::getDocType)
                .collect(java.util.stream.Collectors.toSet());

        if (verifiedTypes.containsAll(REQUIRED_DOCS)) {
            labourServiceClient.markKycVerified(labourId);
            eventPublisher.publishKycFullyVerified(labourId);
        }
    }
}
