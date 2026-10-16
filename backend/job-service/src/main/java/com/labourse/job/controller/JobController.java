package com.labourse.job.controller;

import com.labourse.job.dto.ExpressInterestRequest;
import com.labourse.job.dto.JobPostRequest;
import com.labourse.job.entity.JobInterest;
import com.labourse.job.entity.JobPost;
import com.labourse.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public JobPost postJob(@RequestHeader("X-User-Id") Long customerId,
                            @Valid @RequestBody JobPostRequest req) {
        return jobService.postJob(customerId, req);
    }

    // Labour marks interest — replaces the old instant /accept. Does not lock the job.
    @PostMapping("/{jobId}/interest")
    public JobInterest expressInterest(@RequestHeader("X-User-Id") Long labourId,
                                        @PathVariable Long jobId,
                                        @RequestBody(required = false) ExpressInterestRequest req) {
        return jobService.expressInterest(jobId, labourId, req);
    }

    // Customer browses everyone who has expressed interest so far
    @GetMapping("/{jobId}/interested")
    public List<JobInterest> getInterestedLabour(@PathVariable Long jobId) {
        return jobService.getInterestedLabour(jobId);
    }

    // Customer picks one from the interested list — this is the only point the job actually locks
    @PostMapping("/{jobId}/select")
    public JobPost selectLabour(@RequestHeader("X-User-Id") Long customerId,
                                 @PathVariable Long jobId,
                                 @RequestParam Long labourId) {
        return jobService.selectLabour(jobId, customerId, labourId);
    }

    @PostMapping("/{jobId}/complete")
    public JobPost completeJob(@RequestHeader("X-User-Id") Long customerId, @PathVariable Long jobId) {
        return jobService.completeJob(jobId, customerId);
    }

    @GetMapping("/mine")
    public List<JobPost> myJobs(@RequestHeader("X-User-Id") Long customerId) {
        return jobService.myJobs(customerId);
    }

    @GetMapping("/{jobId}")
    public JobPost getJob(@PathVariable Long jobId) {
        return jobService.getById(jobId);
    }
}
