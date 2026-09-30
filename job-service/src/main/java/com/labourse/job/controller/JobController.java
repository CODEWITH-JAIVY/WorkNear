package com.labourse.job.controller;

import com.labourse.job.dto.JobPostRequest;
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

    @PostMapping("/{jobId}/accept")
    public JobPost acceptJob(@RequestHeader("X-User-Id") Long labourId, @PathVariable Long jobId) {
        return jobService.acceptJob(jobId, labourId);
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
