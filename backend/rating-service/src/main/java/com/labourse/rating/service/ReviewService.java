package com.labourse.rating.service;

import com.labourse.rating.client.LabourServiceClient;
import com.labourse.rating.dto.SubmitReviewRequest;
import com.labourse.rating.entity.Review;
import com.labourse.rating.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository repository;
    private final LabourServiceClient labourServiceClient;

    @Transactional
    public Review submit(Long reviewerId, SubmitReviewRequest req) {
        repository.findByJobIdAndReviewerId(req.getJobId(), reviewerId).ifPresent(r -> {
            throw new IllegalStateException("Already reviewed this job");
        });

        Review review = new Review();
        review.setJobId(req.getJobId());
        review.setReviewerId(reviewerId);
        review.setRevieweeId(req.getRevieweeId());
        review.setReviewerType(req.getReviewerType());
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review = repository.save(review);

        // Only propagate to labour-service's cache when a customer is rating a labour
        if ("CUSTOMER".equals(req.getReviewerType())) {
            Double avg = repository.averageRatingFor(req.getRevieweeId());
            long count = repository.countFor(req.getRevieweeId());
            labourServiceClient.updateCachedRating(req.getRevieweeId(), avg == null ? 0 : avg, count);
        }

        return review;
    }
}
