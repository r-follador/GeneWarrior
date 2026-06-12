package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.ToolJobDto;
import com.genewarrior.genewarrior.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncToolApiController {
    private final JobService jobService;

    public AsyncToolApiController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/api/alignments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ToolJobDto createAlignmentJob() {
        return jobService.createQueued("alignment");
    }

    @PostMapping("/api/primers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ToolJobDto createPrimerJob() {
        return jobService.createQueued("primer3");
    }

    @PostMapping("/api/logos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ToolJobDto createLogoJob() {
        return jobService.createQueued("weblogo");
    }
}
