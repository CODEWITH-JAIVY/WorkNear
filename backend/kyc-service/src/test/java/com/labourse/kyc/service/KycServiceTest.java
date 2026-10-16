package com.labourse.kyc.service;

import com.labourse.kyc.client.LabourServiceClient;
import com.labourse.kyc.dto.ReviewDocumentRequest;
import com.labourse.kyc.entity.DocType;
import com.labourse.kyc.entity.KycDocument;
import com.labourse.kyc.entity.KycStatus;
import com.labourse.kyc.event.KycEventPublisher;
import com.labourse.kyc.repository.KycDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock KycDocumentRepository repository;
    @Mock LabourServiceClient labourServiceClient;
    @Mock KycEventPublisher eventPublisher;
    @InjectMocks KycService kycService;

    private KycDocument doc(Long labourId, DocType type, KycStatus status) {
        KycDocument d = new KycDocument();
        d.setLabourId(labourId);
        d.setDocType(type);
        d.setStatus(status);
        return d;
    }

    @Test
    void review_doesNotPromote_whenOnlySomeRequiredDocsAreVerified() {
        KycDocument aadhaar = doc(1L, DocType.AADHAAR, KycStatus.PENDING);
        aadhaar.setId(100L);

        when(repository.findById(100L)).thenReturn(Optional.of(aadhaar));
        when(repository.save(any(KycDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        // Only AADHAAR verified so far — PAN and SELFIE still pending/missing
        when(repository.findByLabourId(1L)).thenReturn(List.of(
                doc(1L, DocType.AADHAAR, KycStatus.VERIFIED)
        ));

        ReviewDocumentRequest req = new ReviewDocumentRequest();
        req.setApproved(true);

        kycService.review(100L, 999L, req);

        // Not all 3 required docs verified yet — must NOT promote
        verifyNoInteractions(labourServiceClient);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void review_promotesLabour_whenAllRequiredDocsBecomeVerified() {
        KycDocument selfie = doc(2L, DocType.SELFIE, KycStatus.PENDING);
        selfie.setId(200L);

        when(repository.findById(200L)).thenReturn(Optional.of(selfie));
        when(repository.save(any(KycDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        // AADHAAR + PAN already verified earlier, SELFIE is the last one being approved now
        when(repository.findByLabourId(2L)).thenReturn(List.of(
                doc(2L, DocType.AADHAAR, KycStatus.VERIFIED),
                doc(2L, DocType.PAN, KycStatus.VERIFIED),
                doc(2L, DocType.SELFIE, KycStatus.VERIFIED)
        ));

        ReviewDocumentRequest req = new ReviewDocumentRequest();
        req.setApproved(true);

        kycService.review(200L, 999L, req);

        verify(labourServiceClient).markKycVerified(2L);
        verify(eventPublisher).publishKycFullyVerified(2L);
    }

    @Test
    void review_doesNotPromote_whenDocumentIsRejected() {
        KycDocument pan = doc(3L, DocType.PAN, KycStatus.PENDING);
        pan.setId(300L);

        when(repository.findById(300L)).thenReturn(Optional.of(pan));
        when(repository.save(any(KycDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDocumentRequest req = new ReviewDocumentRequest();
        req.setApproved(false);
        req.setNotes("Blurry image, resubmit");

        kycService.review(300L, 999L, req);

        // Rejection path never even checks promotion eligibility
        verify(repository, never()).findByLabourId(any());
        verifyNoInteractions(labourServiceClient);
    }
}
