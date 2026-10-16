package com.labourse.job.service;

import com.labourse.job.client.CatalogServiceClient;
import com.labourse.job.client.MatchingServiceClient;
import com.labourse.job.dto.ExpressInterestRequest;
import com.labourse.job.dto.JobPostRequest;
import com.labourse.job.entity.JobInterest;
import com.labourse.job.entity.JobPost;
import com.labourse.job.entity.JobStatus;
import com.labourse.job.event.JobEventPublisher;
import com.labourse.job.repository.JobInterestRepository;
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

// Covers the browse-and-choose flow: many labour can express interest, only the customer's
// final selectLabour() call locks the job. The atomic Redis-lock guarantee itself (guarding
// against a double-tap on select) is integration-tested against real Redis via Testcontainers
// in JobAcceptanceConcurrencyIT — not unit-testable here since Mockito can't fake Lua atomicity.
@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobPostRepository repository;
    @Mock JobInterestRepository interestRepository;
    @Mock MatchingServiceClient matchingServiceClient;
    @Mock JobEventPublisher eventPublisher;
    @Mock JobAcceptanceService acceptanceService;
    @Mock CatalogServiceClient catalogServiceClient;

    @InjectMocks JobService jobService;

    // ---------- expressInterest ----------

    @Test
    void expressInterest_savesInterestAndNotifiesCustomer_whenJobIsOpen() {
        JobPost job = new JobPost();
        job.setId(10L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(10L)).thenReturn(Optional.of(job));
        when(interestRepository.existsByJobIdAndLabourId(10L, 5L)).thenReturn(false);
        when(interestRepository.save(any(JobInterest.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpressInterestRequest req = new ExpressInterestRequest();
        req.setMessage("Can come by 4pm, ₹350");

        JobInterest result = jobService.expressInterest(10L, 5L, req);

        assertThat(result.getJobId()).isEqualTo(10L);
        assertThat(result.getLabourId()).isEqualTo(5L);
        verify(eventPublisher).publishJobInterestExpressed(10L, 1L, 5L);
        verifyNoInteractions(acceptanceService);
    }

    @Test
    void expressInterest_throws_whenJobNotOpen() {
        JobPost job = new JobPost();
        job.setId(11L);
        job.setStatus(JobStatus.ACCEPTED);

        when(repository.findById(11L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.expressInterest(11L, 6L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer open");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void expressInterest_throws_whenSameLabourAlreadyInterested() {
        JobPost job = new JobPost();
        job.setId(12L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(12L)).thenReturn(Optional.of(job));
        when(interestRepository.existsByJobIdAndLabourId(12L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> jobService.expressInterest(12L, 7L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Already expressed interest");
    }

    // ---------- selectLabour ----------

    @Test
    void selectLabour_succeeds_whenInterestedAndLockWins() {
        JobPost job = new JobPost();
        job.setId(20L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(20L)).thenReturn(Optional.of(job));
        when(interestRepository.existsByJobIdAndLabourId(20L, 5L)).thenReturn(true);
        when(acceptanceService.trySelect(20L, 5L)).thenReturn(true);
        when(repository.save(any(JobPost.class))).thenAnswer(inv -> inv.getArgument(0));
        when(interestRepository.findByJobIdOrderByCreatedAtAsc(20L)).thenReturn(List.of(
                interestOf(20L, 5L), interestOf(20L, 8L), interestOf(20L, 9L)
        ));

        JobPost result = jobService.selectLabour(20L, 1L, 5L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.ACCEPTED);
        assertThat(result.getAcceptedLabourId()).isEqualTo(5L);
        verify(eventPublisher).publishJobAssigned(20L, 1L, 5L, List.of(8L, 9L));
    }

    @Test
    void selectLabour_throws_whenCallerIsNotTheJobsOwnCustomer() {
        JobPost job = new JobPost();
        job.setId(21L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(21L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.selectLabour(21L, 999L, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own customer");

        verifyNoInteractions(acceptanceService);
    }

    @Test
    void selectLabour_throws_whenChosenLabourNeverExpressedInterest() {
        JobPost job = new JobPost();
        job.setId(22L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(22L)).thenReturn(Optional.of(job));
        when(interestRepository.existsByJobIdAndLabourId(22L, 999L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.selectLabour(22L, 1L, 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has not expressed interest");

        verifyNoInteractions(acceptanceService);
    }

    @Test
    void selectLabour_throws_whenDoubleTapLosesTheLock() {
        JobPost job = new JobPost();
        job.setId(23L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.OPEN);

        when(repository.findById(23L)).thenReturn(Optional.of(job));
        when(interestRepository.existsByJobIdAndLabourId(23L, 5L)).thenReturn(true);
        when(acceptanceService.trySelect(23L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.selectLabour(23L, 1L, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in progress");

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void selectLabour_throws_whenJobAlreadyAssigned() {
        JobPost job = new JobPost();
        job.setId(24L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.ACCEPTED);

        when(repository.findById(24L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.selectLabour(24L, 1L, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer open");

        verifyNoInteractions(acceptanceService);
    }

    // ---------- postJob (unchanged behaviour, still covered) ----------

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
        when(matchingServiceClient.findNearbyLabour(eq(99L), eq(req))).thenReturn(List.of());

        jobService.postJob(1L, req);

        verify(eventPublisher).publishJobPosted(eq(99L), any());
    }

    @Test
    void postJob_persistsSubSkillAndGenderPreferenceOntoTheJobPost() {
        JobPostRequest req = new JobPostRequest();
        req.setTitle("Kitchen deep clean");
        req.setRequiredLabourType("CLEANING");
        req.setSubSkill("KITCHEN_CLEANING");
        req.setGenderPreference("FEMALE");
        req.setLatitude(28.5);
        req.setLongitude(77.3);

        when(repository.save(any(JobPost.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchingServiceClient.findNearbyLabour(any(), eq(req))).thenReturn(List.of());

        JobPost saved = jobService.postJob(1L, req);

        assertThat(saved.getSubSkill()).isEqualTo("KITCHEN_CLEANING");
        assertThat(saved.getGenderPreference()).isEqualTo("FEMALE");
    }

    @Test
    void postJob_throwsAndNeverSaves_whenCatalogRejectsTheServiceCode() {
        JobPostRequest req = new JobPostRequest();
        req.setTitle("Fix something");
        req.setRequiredLabourType("NOT_A_REAL_CODE");
        req.setLatitude(28.5);
        req.setLongitude(77.3);

        doThrow(new IllegalArgumentException("Unknown service code: NOT_A_REAL_CODE"))
                .when(catalogServiceClient).validateCodeOrThrow("NOT_A_REAL_CODE");

        assertThatThrownBy(() -> jobService.postJob(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown service code");

        verify(repository, never()).save(any());
        verifyNoInteractions(matchingServiceClient, eventPublisher);
    }

    // ---------- completeJob + attachment retention ----------

    @Test
    void completeJob_marksCompletedAndTriggersMediaDeletion_whenAttachmentsExist() {
        JobPost job = new JobPost();
        job.setId(30L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.ACCEPTED);
        job.setMediaKeysCsv("job-attachments/a.jpg,job-attachments/b.mp4");

        when(repository.findById(30L)).thenReturn(Optional.of(job));
        when(repository.save(any(JobPost.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPost result = jobService.completeJob(30L, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        verify(eventPublisher).publishJobCompleted(30L, List.of("job-attachments/a.jpg", "job-attachments/b.mp4"));
    }

    @Test
    void completeJob_publishesNoDeletionEvent_whenJobHadNoAttachments() {
        JobPost job = new JobPost();
        job.setId(31L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.ACCEPTED);

        when(repository.findById(31L)).thenReturn(Optional.of(job));
        when(repository.save(any(JobPost.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.completeJob(31L, 1L);

        verify(eventPublisher, never()).publishJobCompleted(any(), any());
    }

    @Test
    void completeJob_throws_whenCallerIsNotTheOwningCustomer() {
        JobPost job = new JobPost();
        job.setId(32L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.ACCEPTED);
        when(repository.findById(32L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.completeJob(32L, 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own customer");
        verify(repository, never()).save(any());
    }

    @Test
    void completeJob_throws_whenJobWasNeverAssigned() {
        JobPost job = new JobPost();
        job.setId(33L);
        job.setCustomerId(1L);
        job.setStatus(JobStatus.OPEN);
        when(repository.findById(33L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.completeJob(33L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assigned");
    }

    @Test
    void postJob_rejectsMoreThanFiveAttachments() {
        JobPostRequest req = new JobPostRequest();
        req.setTitle("Big job");
        req.setRequiredLabourType("HOME_REPAIR__ELECTRICIAN");
        req.setLatitude(28.5);
        req.setLongitude(77.3);
        req.setMediaKeys(List.of("a", "b", "c", "d", "e", "f"));

        assertThatThrownBy(() -> jobService.postJob(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At most 5");
        verify(repository, never()).save(any());
    }

    @Test
    void postJob_persistsScheduledTimeAndMediaKeys() {
        JobPostRequest req = new JobPostRequest();
        req.setTitle("Fix fan");
        req.setRequiredLabourType("HOME_REPAIR__FAN_INSTALLATION_REPAIR");
        req.setLatitude(28.5);
        req.setLongitude(77.3);
        java.time.LocalDateTime when = java.time.LocalDateTime.of(2026, 10, 12, 10, 0);
        req.setScheduledAt(when);
        req.setMediaKeys(List.of("job-attachments/x.jpg", "job-attachments/y.m4a"));

        when(repository.save(any(JobPost.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchingServiceClient.findNearbyLabour(any(), eq(req))).thenReturn(List.of());

        JobPost saved = jobService.postJob(1L, req);

        assertThat(saved.getScheduledAt()).isEqualTo(when);
        assertThat(saved.getMediaKeysCsv()).isEqualTo("job-attachments/x.jpg,job-attachments/y.m4a");
    }

    private JobInterest interestOf(Long jobId, Long labourId) {
        JobInterest i = new JobInterest();
        i.setJobId(jobId);
        i.setLabourId(labourId);
        return i;
    }
}
