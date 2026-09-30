package com.labourse.job.service;

import com.labourse.job.client.MatchingServiceClient;
import com.labourse.job.dto.JobPostRequest;
import com.labourse.job.entity.JobPost;
import com.labourse.job.entity.JobStatus;
import com.labourse.job.event.JobEventPublisher;
import com.labourse.job.repository.JobPostRepository;
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

// Covers the race-condition logic at the JobService layer — the actual atomicity guarantee
// comes from the Redis Lua script (integration-tested against a real Redis via Testcontainers
// in JobAcceptanceServiceIT, not unit-testable here since Mockito can't fake Lua atomicity).
@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobPostRepository repository;
    @Mock MatchingServiceClient matchingServiceClient;
    @Mock JobEventPublisher eventPublisher;
    @Mock JobAcceptanceService acceptanceService;

    @InjectMocks JobService jobService;

    @Test
    void acceptJob_succeeds_whenLabourWinsTheRace() {
        JobPost job = new JobPost();
        job.setId(10L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(10L)).thenReturn(Optional.of(job));
        when(acceptanceService.tryAccept(10L, 5L)).thenReturn(true);
        when(repository.save(any(JobPost.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPost result = jobService.acceptJob(10L, 5L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.ACCEPTED);
        assertThat(result.getAcceptedLabourId()).isEqualTo(5L);
        verify(eventPublisher).publishJobAccepted(10L, 1L, 5L);
    }

    @Test
    void acceptJob_throws_whenAnotherLabourAlreadyWonTheRace() {
        JobPost job = new JobPost();
        job.setId(11L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(11L)).thenReturn(Optional.of(job));
        when(acceptanceService.tryAccept(11L, 6L)).thenReturn(false); // lost the atomic race

        assertThatThrownBy(() -> jobService.acceptJob(11L, 6L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already accepted");

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void acceptJob_throws_whenJobAlreadyNotOpen() {
        JobPost job = new JobPost();
        job.setId(12L);
        job.setStatus(JobStatus.COMPLETED);

        when(repository.findById(12L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.acceptJob(12L, 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer open");

        verifyNoInteractions(acceptanceService); // never even attempt the Redis race if already closed
    }

    @Test
    void postJob_dispatchesToMatchingServiceAndPublishesJobPostedEvent() {
        JobPostRequest req = new JobPostRequest();
        req.setTitle("Fix leaking tap");
        req.setRequiredLabourType("PLUMBER");
        req.setLatitude(28.5);
        req.setLongitude(77.3);

        when(repository.save(any(JobPost.class))).thenAnswer(inv -> {
            JobPost j = inv.getArgument(0);
            j.setId(99L);
            return j;
        });
        when(matchingServiceClient.findNearbyLabour(eq(99L), eq(req)))
                .thenReturn(List.of());

        jobService.postJob(1L, req);

        verify(eventPublisher).publishJobPosted(eq(99L), any());
    }
}
