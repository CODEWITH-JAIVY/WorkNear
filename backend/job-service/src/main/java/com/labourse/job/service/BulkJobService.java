package com.labourse.job.service;

import com.labourse.job.client.CatalogServiceClient;
import com.labourse.job.client.MatchingServiceClient;
import com.labourse.job.dto.*;
import com.labourse.job.entity.BulkJobInterest;
import com.labourse.job.entity.BulkJobPost;
import com.labourse.job.entity.BulkJobStatus;
import com.labourse.job.event.JobEventPublisher;
import com.labourse.job.repository.BulkJobInterestRepository;
import com.labourse.job.repository.BulkJobPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BulkJobService {

    private final BulkJobPostRepository repository;
    private final BulkJobInterestRepository interestRepository;
    private final MatchingServiceClient matchingServiceClient;
    private final JobEventPublisher eventPublisher;
    private final CatalogServiceClient catalogServiceClient;

    // No payment/commission step here, unlike JobService.postJob — this channel monetizes via
    // ads in the worker feed (see BulkJobFeedResponse.adSlot), not a cut of the hiring.
    @Transactional
    public BulkJobPost postBulkJob(Long contractorId, BulkJobPostRequest req) {
        catalogServiceClient.validateCodeOrThrow(req.getRequiredLabourType());

        BulkJobPost job = new BulkJobPost();
        job.setContractorId(contractorId);
        job.setRequiredLabourType(req.getRequiredLabourType());
        job.setWorkersNeeded(req.getWorkersNeeded());
        job.setDescription(req.getDescription());
        job.setSiteAddressText(req.getSiteAddressText());
        job.setLatitude(req.getLatitude());
        job.setLongitude(req.getLongitude());
        job.setRadiusKm(req.getRadiusKm());
        job.setExternalFormLink(req.getExternalFormLink());
        job = repository.save(job);

        List<NearbyLabourDto> matches = matchingServiceClient.findNearbyLabourForBulkJob(job.getId(), req);
        List<String> candidateIds = matches.stream().map(NearbyLabourDto::getLabourId).toList();
        eventPublisher.publishBulkJobPosted(job.getId(), candidateIds);

        return job;
    }

    // Worker-facing feed — nearby OPEN bulk requirements matching their trade. adSlot is always
    // null today (see BulkJobFeedResponse); fills in once an ad system exists, no API-shape change.
    public BulkJobFeedResponse getNearbyFeed(String labourType, double lat, double lon, double radiusKm) {
        // Simple distance filter in-process for now — if the bulk-job volume grows large enough
        // to matter, move this to matching-service's Redis GEO index like the single-job flow.
        List<BulkJobFeedItemDto> items = repository.findAll().stream()
                .filter(j -> j.getStatus() == BulkJobStatus.OPEN)
                .filter(j -> j.getRequiredLabourType().equalsIgnoreCase(labourType))
                .filter(j -> distanceKm(lat, lon, j.getLatitude(), j.getLongitude()) <= radiusKm)
                .map(j -> new BulkJobFeedItemDto(j.getId(), j.getRequiredLabourType(), j.getWorkersNeeded(),
                        j.getDescription(), j.getSiteAddressText(), j.getExternalFormLink(), j.getCreatedAt()))
                .toList();

        return new BulkJobFeedResponse(items, null);
    }

    // Fallback path when the contractor gave no externalFormLink — a lightweight in-app mark.
    @Transactional
    public BulkJobInterest expressInterest(Long bulkJobId, Long labourId) {
        BulkJobPost job = repository.findById(bulkJobId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk job not found"));

        if (job.getStatus() != BulkJobStatus.OPEN) {
            throw new IllegalStateException("This bulk requirement is no longer open");
        }
        if (job.getExternalFormLink() != null && !job.getExternalFormLink().isBlank()) {
            throw new IllegalStateException("This job uses an external form — apply there instead");
        }
        if (interestRepository.existsByBulkJobIdAndLabourId(bulkJobId, labourId)) {
            throw new IllegalStateException("Already expressed interest in this bulk job");
        }

        BulkJobInterest interest = new BulkJobInterest();
        interest.setBulkJobId(bulkJobId);
        interest.setLabourId(labourId);
        return interestRepository.save(interest);
    }

    public List<BulkJobInterest> getInterested(Long bulkJobId) {
        return interestRepository.findByBulkJobIdOrderByCreatedAtAsc(bulkJobId);
    }

    @Transactional
    public BulkJobPost close(Long bulkJobId, Long contractorId) {
        BulkJobPost job = repository.findById(bulkJobId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk job not found"));
        if (!job.getContractorId().equals(contractorId)) {
            throw new IllegalStateException("Only the posting contractor can close this");
        }
        job.setStatus(BulkJobStatus.CLOSED);
        return repository.save(job);
    }

    public List<BulkJobPost> myBulkJobs(Long contractorId) {
        return repository.findByContractorIdOrderByCreatedAtDesc(contractorId);
    }

    // Haversine — fine at this scale (in-process filter over a reasonably small bulk-job table);
    // revisit with Redis GEO if bulk-job volume ever grows into the thousands.
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
