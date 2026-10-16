package com.labourse.media.controller;

import com.labourse.media.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    // The folder used to be any client-supplied string. Now an allowlist: retention (2-day S3
    // lifecycle + delete-on-complete) hangs off the job-attachments/ prefix, so a client must
    // not be able to write arbitrary prefixes — or park permanent files under the expiring one.
    private static final List<String> ALLOWED_FOLDERS =
            List.of("profile-photos", "work-photos", "kyc-docs", "job-attachments");

    private final S3Service s3Service;

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam MultipartFile file, @RequestParam String folder) {
        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown upload folder: " + folder);
        }
        if ("job-attachments".equals(folder) && !isPhotoVideoOrAudio(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Job attachments must be a photo, video or voice note");
        }

        S3Service.UploadResult result = s3Service.uploadWithKey(file, folder);
        // key is what job-service stores (and later asks us to delete); url is what the app displays
        return Map.of("url", result.url(), "key", result.key());
    }

    private boolean isPhotoVideoOrAudio(String contentType) {
        return contentType != null && (contentType.startsWith("image/")
                || contentType.startsWith("video/") || contentType.startsWith("audio/"));
    }
}
