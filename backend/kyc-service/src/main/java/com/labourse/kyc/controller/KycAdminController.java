package com.labourse.kyc.controller;

import com.labourse.kyc.dto.ReviewDocumentRequest;
import com.labourse.kyc.entity.KycDocument;
import com.labourse.kyc.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Should only be reachable by ADMIN role — enforce at the gateway (X-User-Role check) before prod
@RestController
@RequestMapping("/api/kyc/admin")
@RequiredArgsConstructor
public class KycAdminController {

    private final KycService kycService;

    @GetMapping("/pending")
    public List<KycDocument> pending() {
        return kycService.pendingReview();
    }

    @PostMapping("/{docId}/review")
    public KycDocument review(@RequestHeader("X-User-Id") Long adminId,
                               @PathVariable Long docId, @RequestBody ReviewDocumentRequest req) {
        return kycService.review(docId, adminId, req);
    }
}
