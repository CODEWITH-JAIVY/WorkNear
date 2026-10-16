package com.labourse.job.service;

import com.labourse.job.client.CatalogServiceClient;
import com.labourse.job.client.MatchingServiceClient;
import com.labourse.job.dto.BulkJobFeedResponse;
import com.labourse.job.dto.BulkJobPostRequest;
import com.labourse.job.dto.NearbyLabourDto;
import com.labourse.job.entity.BulkJobPost;
import com.labourse.job.entity.BulkJobStatus;
import com.labourse.job.event.JobEventPublisher;
import com.labourse.job.repository.BulkJobInterestRepository;
import com.labourse.job.repository.BulkJobPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkJobServiceTest {

    @Mock BulkJobPostRepository repository;
    @Mock BulkJobInterestRepository interestRepository;
    @Mock MatchingServiceClient matchingServiceClient;
    @Mock JobEventPublisher eventPublisher;
    @Mock CatalogServiceClient catalogServiceClient;

    @InjectMocks BulkJobService bulkJobService;

    @Test
    void postBulkJob_doesNotTouchPayment_andNotifiesMatchedCandidates() {
        BulkJobPostRequest req = new BulkJobPostRequest();
        req.setRequiredLabourType("MASON");
        req.setWorkersNeeded(10);
        req.setLatitude(28.5);
        req.setLongitude(77.3);

        when(repository.save(any(BulkJobPost.class))).thenAnswer(inv -> {
            BulkJobPost j = inv.getArgument(0);
            j.setId(50L);
            return j;
        });
        NearbyLabourDto candidate1 = new NearbyLabourDto();
        candidate1.setLabourId("201");
        NearbyLabourDto candidate2 = new NearbyLabourDto();
        candidate2.setLabourId("202");

        when(matchingServiceClient.findNearbyLabourForBulkJob(eq(50L), eq(req)))
                .thenReturn(List.of(candidate1, candidate2));

        BulkJobPost result = bulkJobService.postBulkJob(1L, req);

        assertThat(result.getContractorId()).isEqualTo(1L);
        assertThat(result.getWorkersNeeded()).isEqualTo(10);
        verify(eventPublisher).publishBulkJobPosted(eq(50L), eq(List.of("201", "202")));
        // No commission/payment service exists in this service at all — nothing to verify an
        // absence of, but the point stands: postBulkJob's only side effects are save + notify.
    }

    @Test
    void getNearbyFeed_returnsNullAdSlot_asThePlaceholderForFutureAds() {
        when(repository.findAll()).thenReturn(List.of());

        BulkJobFeedResponse response = bulkJobService.getNearbyFeed("MASON", 28.5, 77.3, 5.0);

        assertThat(response.getAdSlot()).isNull();
        assertThat(response.getJobs()).isEmpty();
    }

    @Test
    void getNearbyFeed_excludesClosedJobs() {
        BulkJobPost openJob = bulkJob(1L, "MASON", BulkJobStatus.OPEN, 28.5, 77.3);
        BulkJobPost closedJob = bulkJob(2L, "MASON", BulkJobStatus.CLOSED, 28.5, 77.3);
        when(repository.findAll()).thenReturn(List.of(openJob, closedJob));

        BulkJobFeedResponse response = bulkJobService.getNearbyFeed("MASON", 28.5, 77.3, 5.0);

        assertThat(response.getJobs()).hasSize(1);
        assertThat(response.getJobs().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getNearbyFeed_excludesJobsOutsideRadius() {
        // ~Delhi vs ~Mumbai — roughly 1150km apart, well outside any 5km radius
        BulkJobPost farJob = bulkJob(3L, "MASON", BulkJobStatus.OPEN, 19.0760, 72.8777);
        when(repository.findAll()).thenReturn(List.of(farJob));

        BulkJobFeedResponse response = bulkJobService.getNearbyFeed("MASON", 28.6139, 77.2090, 5.0);

        assertThat(response.getJobs()).isEmpty();
    }

    @Test
    void expressInterest_throws_whenJobHasAnExternalFormLink() {
        BulkJobPost job = bulkJob(4L, "MASON", BulkJobStatus.OPEN, 28.5, 77.3);
        job.setExternalFormLink("https://forms.gle/abc123");

        when(repository.findById(4L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> bulkJobService.expressInterest(4L, 201L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external form");

        verifyNoInteractions(interestRepository);
    }

    @Test
    void expressInterest_succeeds_whenNoExternalFormLink() {
        BulkJobPost job = bulkJob(5L, "MASON", BulkJobStatus.OPEN, 28.5, 77.3);
        job.setExternalFormLink(null);

        when(repository.findById(5L)).thenReturn(Optional.of(job));
        when(interestRepository.existsByBulkJobIdAndLabourId(5L, 201L)).thenReturn(false);
        when(interestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = bulkJobService.expressInterest(5L, 201L);

        assertThat(result.getBulkJobId()).isEqualTo(5L);
        assertThat(result.getLabourId()).isEqualTo(201L);
    }

    @Test
    void close_throws_whenCallerIsNotThePostingContractor() {
        BulkJobPost job = bulkJob(6L, "MASON", BulkJobStatus.OPEN, 28.5, 77.3);
        job.setContractorId(1L);

        when(repository.findById(6L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> bulkJobService.close(6L, 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("posting contractor");
    }

    private BulkJobPost bulkJob(Long id, String type, BulkJobStatus status, double lat, double lon) {
        BulkJobPost j = new BulkJobPost();
        j.setId(id);
        j.setRequiredLabourType(type);
        j.setStatus(status);
        j.setLatitude(lat);
        j.setLongitude(lon);
        j.setRadiusKm(5.0);
        return j;
    }
}
