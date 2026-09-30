package com.labourse.kyc.dto;

import com.labourse.kyc.entity.DocType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitDocumentRequest {
    @NotNull private DocType docType;
    @NotBlank private String documentUrl; // returned by media-service's /api/media/upload
}
