package com.labourse.rating.controller;

import com.labourse.rating.dto.SubmitReviewRequest;
import com.labourse.rating.entity.Review;
import com.labourse.rating.repository.ReviewRepository;
import com.labourse.rating.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository repository;

    @PostMapping
    public Review submit(@RequestHeader("X-User-Id") Long reviewerId, @Valid @RequestBody SubmitReviewRequest req) {
        return reviewService.submit(reviewerId, req);
    }

    @GetMapping("/for/{userId}")
    public List<Review> getReviewsFor(@PathVariable Long userId) {
        return repository.findByRevieweeId(userId);
    }
}
