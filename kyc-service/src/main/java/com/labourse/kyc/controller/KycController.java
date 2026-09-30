package com.labourse.kyc.controller;

import com.labourse.kyc.dto.SubmitDocumentRequest;
import com.labourse.kyc.entity.KycDocument;
import com.labourse.kyc.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping("/documents")
    public KycDocument submit(@RequestHeader("X-User-Id") Long labourId, @Valid @RequestBody SubmitDocumentRequest req) {
        return kycService.submit(labourId, req);
    }

    @GetMapping("/documents/mine")
    public List<KycDocument> myDocuments(@RequestHeader("X-User-Id") Long labourId) {
        return kycService.forLabour(labourId);
    }
}
