package com.labourse.kyc.repository;

import com.labourse.kyc.entity.KycDocument;
import com.labourse.kyc.entity.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByLabourId(Long labourId);
    List<KycDocument> findByStatus(KycStatus status);
}
