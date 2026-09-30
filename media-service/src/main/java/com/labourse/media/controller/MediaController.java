package com.labourse.media.controller;

import com.labourse.media.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final S3Service s3Service;

    // folder: "profile-photos" | "work-photos" | "kyc-docs"
    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam MultipartFile file, @RequestParam String folder) {
        return Map.of("url", s3Service.upload(file, folder));
    }
}
