package com.labourse.rating.service;

import com.labourse.rating.client.LabourServiceClient;
import com.labourse.rating.dto.SubmitReviewRequest;
import com.labourse.rating.entity.Review;
import com.labourse.rating.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository repository;
    @Mock LabourServiceClient labourServiceClient;
    @InjectMocks ReviewService reviewService;

    @Test
    void submit_throws_whenReviewerAlreadyReviewedThisJob() {
        SubmitReviewRequest req = new SubmitReviewRequest();
        req.setJobId(1L);
        req.setRevieweeId(5L);
        req.setReviewerType("CUSTOMER");
        req.setRating(5);

        when(repository.findByJobIdAndReviewerId(1L, 2L)).thenReturn(Optional.of(new Review()));

        assertThatThrownBy(() -> reviewService.submit(2L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Already reviewed");

        verifyNoInteractions(labourServiceClient);
    }

    @Test
    void submit_propagatesRatingToLabourService_onlyWhenCustomerRatesLabour() {
        SubmitReviewRequest req = new SubmitReviewRequest();
        req.setJobId(1L);
        req.setRevieweeId(5L);
        req.setReviewerType("CUSTOMER");
        req.setRating(4);

        when(repository.findByJobIdAndReviewerId(1L, 2L)).thenReturn(Optional.empty());
        when(repository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.averageRatingFor(5L)).thenReturn(4.3);
        when(repository.countFor(5L)).thenReturn(10L);

        reviewService.submit(2L, req);

        verify(labourServiceClient).updateCachedRating(5L, 4.3, 10L);
    }

    @Test
    void submit_doesNotPropagateRating_whenLabourRatesCustomer() {
        SubmitReviewRequest req = new SubmitReviewRequest();
        req.setJobId(1L);
        req.setRevieweeId(9L);
        req.setReviewerType("LABOUR"); // labour rating a customer — no rating cache to update
        req.setRating(5);

        when(repository.findByJobIdAndReviewerId(1L, 3L)).thenReturn(Optional.empty());
        when(repository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submit(3L, req);

        verifyNoInteractions(labourServiceClient);
    }
}
