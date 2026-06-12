package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.AlignmentDto;
import com.genewarrior.genewarrior.api.dto.PairwiseAlignmentRequest;
import com.genewarrior.genewarrior.api.dto.ToolJobDto;
import com.genewarrior.genewarrior.service.AlignmentService;
import com.genewarrior.genewarrior.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncToolApiController {
    private final JobService jobService;
    private final AlignmentService alignmentService;

    public AsyncToolApiController(JobService jobService, AlignmentService alignmentService) {
        this.jobService = jobService;
        this.alignmentService = alignmentService;
    }

    @PostMapping("/api/alignments/pairwise")
    AlignmentDto createPairwiseAlignment(@Valid @RequestBody PairwiseAlignmentRequest request) {
        return alignmentService.pairwise(request.sequences(), request.mode());
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
