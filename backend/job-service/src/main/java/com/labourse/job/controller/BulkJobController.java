package com.labourse.job.controller;

import com.labourse.job.dto.BulkJobFeedResponse;
import com.labourse.job.dto.BulkJobPostRequest;
import com.labourse.job.entity.BulkJobInterest;
import com.labourse.job.entity.BulkJobPost;
import com.labourse.job.service.BulkJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Contractor bulk-hiring flow — same CUSTOMER user-type as single-job posting, just a different
// mode of the same app (see the app's "Single Worker" / "Bulk Hire" toggle). No payment-service
// involvement anywhere in this controller — this channel monetizes via the feed's ad slot, not commission.
@RestController
@RequestMapping("/api/jobs/bulk")
@RequiredArgsConstructor
public class BulkJobController {

    private final BulkJobService bulkJobService;

    @PostMapping
    public BulkJobPost postBulkJob(@RequestHeader("X-User-Id") Long contractorId,
                                    @Valid @RequestBody BulkJobPostRequest req) {
        return bulkJobService.postBulkJob(contractorId, req);
    }

    // Worker's "Bulk Opportunities near you" feed
    @GetMapping("/feed")
    public BulkJobFeedResponse getFeed(@RequestParam String labourType,
                                        @RequestParam double lat,
                                        @RequestParam double lon,
                                        @RequestParam(defaultValue = "5.0") double radiusKm) {
        return bulkJobService.getNearbyFeed(labourType, lat, lon, radiusKm);
    }

    // Only used when the bulk job has no externalFormLink — see BulkJobService.expressInterest
    @PostMapping("/{bulkJobId}/interest")
    public BulkJobInterest expressInterest(@RequestHeader("X-User-Id") Long labourId,
                                            @PathVariable Long bulkJobId) {
        return bulkJobService.expressInterest(bulkJobId, labourId);
    }

    @GetMapping("/{bulkJobId}/interested")
    public List<BulkJobInterest> getInterested(@PathVariable Long bulkJobId) {
        return bulkJobService.getInterested(bulkJobId);
    }

    @PostMapping("/{bulkJobId}/close")
    public BulkJobPost close(@RequestHeader("X-User-Id") Long contractorId, @PathVariable Long bulkJobId) {
        return bulkJobService.close(bulkJobId, contractorId);
    }

    @GetMapping("/mine")
    public List<BulkJobPost> myBulkJobs(@RequestHeader("X-User-Id") Long contractorId) {
        return bulkJobService.myBulkJobs(contractorId);
    }
}
