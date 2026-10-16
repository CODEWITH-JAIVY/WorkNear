package com.labourse.rating.repository;

import com.labourse.rating.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeId(Long revieweeId);
    Optional<Review> findByJobIdAndReviewerId(Long jobId, Long reviewerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.revieweeId = :revieweeId")
    Double averageRatingFor(Long revieweeId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.revieweeId = :revieweeId")
    long countFor(Long revieweeId);
}
