package com.labourse.job.service;

import com.labourse.job.client.MatchingServiceClient;
import com.labourse.job.dto.JobPostRequest;
import com.labourse.job.dto.NearbyLabourDto;
import com.labourse.job.entity.JobPost;
import com.labourse.job.entity.JobStatus;
import com.labourse.job.event.JobEventPublisher;
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
    private final MatchingServiceClient matchingServiceClient;
    private final JobEventPublisher eventPublisher;
    private final JobAcceptanceService acceptanceService;

    @Transactional
    public JobPost postJob(Long customerId, JobPostRequest req) {
        JobPost job = new JobPost();
        job.setCustomerId(customerId);
        job.setTitle(req.getTitle());
        job.setDescription(req.getDescription());
        job.setRequiredLabourType(req.getRequiredLabourType());
        job.setSkillsRequiredCsv(req.getSkillsRequired() == null ? null : String.join(",", req.getSkillsRequired()));
        job.setMinRating(req.getMinRating());
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

    @Transactional
    public JobPost acceptJob(Long jobId, Long labourId) {
        JobPost job = repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalStateException("Job is no longer open");
        }

        // Atomic race — only the first caller to reach Redis wins, even under concurrent requests
        boolean won = acceptanceService.tryAccept(jobId, labourId);
        if (!won) {
            throw new IllegalStateException("Job was already accepted by another labour");
        }

        job.setAcceptedLabourId(labourId);
        job.setStatus(JobStatus.ACCEPTED);
        job.setAcceptedAt(LocalDateTime.now());
        job = repository.save(job);

        eventPublisher.publishJobAccepted(job.getId(), job.getCustomerId(), labourId);
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
