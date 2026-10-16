package com.labourse.job.service;

import com.labourse.job.client.CatalogServiceClient;
import com.labourse.job.client.MatchingServiceClient;
import com.labourse.job.dto.ExpressInterestRequest;
import com.labourse.job.dto.JobPostRequest;
import com.labourse.job.dto.NearbyLabourDto;
import com.labourse.job.entity.JobInterest;
import com.labourse.job.entity.JobPost;
import com.labourse.job.entity.JobStatus;
import com.labourse.job.event.JobEventPublisher;
import com.labourse.job.repository.JobInterestRepository;
import com.labourse.job.repository.JobPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobPostRepository repository;
    private final JobInterestRepository interestRepository;
    private final MatchingServiceClient matchingServiceClient;
    private final JobEventPublisher eventPublisher;
    private final JobAcceptanceService acceptanceService;
    private final CatalogServiceClient catalogServiceClient;

    private static final int MAX_ATTACHMENTS = 5;

    @Transactional
    public JobPost postJob(Long customerId, JobPostRequest req) {
        // requiredLabourType must be a real catalog code — otherwise this job silently never
        // matches anyone, since labour-service's serviceCodes are catalog codes too.
        catalogServiceClient.validateCodeOrThrow(req.getRequiredLabourType());

        JobPost job = new JobPost();
        job.setCustomerId(customerId);
        job.setTitle(req.getTitle());
        job.setDescription(req.getDescription());
        job.setRequiredLabourType(req.getRequiredLabourType());
        job.setSkillsRequiredCsv(req.getSkillsRequired() == null ? null : String.join(",", req.getSkillsRequired()));
        job.setSubSkill(req.getSubSkill());
        job.setGenderPreference(req.getGenderPreference());
        job.setLanguagePreference(req.getLanguagePreference());
        job.setMinRating(req.getMinRating());
        job.setScheduledAt(req.getScheduledAt());
        if (req.getMediaKeys() != null && req.getMediaKeys().size() > MAX_ATTACHMENTS) {
            throw new IllegalArgumentException("At most " + MAX_ATTACHMENTS + " attachments per job");
        }
        job.setMediaKeysCsv(req.getMediaKeys() == null || req.getMediaKeys().isEmpty()
                ? null : String.join(",", req.getMediaKeys()));
        job.setLatitude(req.getLatitude());
        job.setLongitude(req.getLongitude());
        job.setAddressText(req.getAddressText());
        job.setBudget(req.getBudget());
        job = repository.save(job);

        List<NearbyLabourDto> matches = matchingServiceClient.findNearbyLabour(job.getId(), req);
        List<String> candidateIds = matches.stream().map(NearbyLabourDto::getLabourId).toList();

        // job.posted event → notification-service pushes to every matched labour's device
        eventPublisher.publishJobPosted(job.getId(), candidateIds);

        return job;
    }

    // Labour marks interest — does NOT lock the job, multiple labour can do this for the same job.
    @Transactional
    public JobInterest expressInterest(Long jobId, Long labourId, ExpressInterestRequest req) {
        JobPost job = repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalStateException("Job is no longer open for interest");
        }
        if (interestRepository.existsByJobIdAndLabourId(jobId, labourId)) {
            throw new IllegalStateException("Already expressed interest in this job");
        }

        JobInterest interest = new JobInterest();
        interest.setJobId(jobId);
        interest.setLabourId(labourId);
        interest.setMessage(req == null ? null : req.getMessage());
        interest = interestRepository.save(interest);

        // Triggers: customer gets a "someone's interested" push, and chat-service opens a
        // (jobId, labourId) room for this pair to negotiate before any commitment is made.
        eventPublisher.publishJobInterestExpressed(jobId, job.getCustomerId(), labourId);
        return interest;
    }

    // Customer-facing — browse everyone who has expressed interest so far, to chat/call/compare
    public List<JobInterest> getInterestedLabour(Long jobId) {
        return interestRepository.findByJobIdOrderByCreatedAtAsc(jobId);
    }

    // Customer picks one. This is the only point a job actually locks — replaces the old
    // "first labour to accept wins" flow entirely.
    @Transactional
    public JobPost selectLabour(Long jobId, Long customerId, Long selectedLabourId) {
        JobPost job = repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        if (!job.getCustomerId().equals(customerId)) {
            throw new IllegalStateException("Only the job's own customer can select a labour");
        }
        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalStateException("Job is no longer open");
        }
        if (!interestRepository.existsByJobIdAndLabourId(jobId, selectedLabourId)) {
            throw new IllegalStateException("That labour has not expressed interest in this job");
        }

        // Guards against a double-tap on the select button locking two different labour in a race
        boolean locked = acceptanceService.trySelect(jobId, selectedLabourId);
        if (!locked) {
            throw new IllegalStateException("A selection for this job is already in progress");
        }

        job.setAcceptedLabourId(selectedLabourId);
        job.setStatus(JobStatus.ACCEPTED);
        job.setAcceptedAt(LocalDateTime.now());
        job = repository.save(job);

        List<Long> notSelected = interestRepository.findByJobIdOrderByCreatedAtAsc(jobId).stream()
                .map(JobInterest::getLabourId)
                .filter(id -> !id.equals(selectedLabourId))
                .toList();

        eventPublisher.publishJobAssigned(job.getId(), job.getCustomerId(), selectedLabourId, notSelected);
        return job;
    }

    // Customer marks the work done — the only point a job reaches COMPLETED, and the trigger
    // for deleting its photos/video/voice-note straight away.
    @Transactional
    public JobPost completeJob(Long jobId, Long customerId) {
        JobPost job = repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (!job.getCustomerId().equals(customerId)) {
            throw new IllegalStateException("Only the job's own customer can complete it");
        }
        if (job.getStatus() != JobStatus.ACCEPTED && job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an assigned job can be completed");
        }
        job.setStatus(JobStatus.COMPLETED);
        job = repository.save(job);

        List<String> keys = job.getMediaKeysCsv() == null || job.getMediaKeysCsv().isBlank()
                ? List.of() : List.of(job.getMediaKeysCsv().split(","));
        if (!keys.isEmpty()) {
            eventPublisher.publishJobCompleted(job.getId(), keys);
        }
        return job;
    }

    public List<JobPost> myJobs(Long customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public JobPost getById(Long jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }
}
