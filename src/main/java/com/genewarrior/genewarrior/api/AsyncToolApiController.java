package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.AlignmentDto;
import com.genewarrior.genewarrior.api.dto.ConsensusRequest;
import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.PairwiseAlignmentRequest;
import com.genewarrior.genewarrior.api.dto.ToolJobDto;
import com.genewarrior.genewarrior.api.dto.MultipleAlignmentRequest;
import com.genewarrior.genewarrior.service.JobService;
import com.genewarrior.genewarrior.service.TcoffeeService;
import com.genewarrior.genewarrior.service.PrimerDesignService;
import com.genewarrior.genewarrior.api.dto.PrimerDesignRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import com.genewarrior.genewarrior.service.AlignmentService;
import com.genewarrior.genewarrior.service.ExternalToolUnavailableException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncToolApiController {
    private final AlignmentService alignmentService;
    private final JobService jobs;
    private final TcoffeeService tcoffee;
    private final PrimerDesignService primer3;

    public AsyncToolApiController(AlignmentService alignmentService, JobService jobs, TcoffeeService tcoffee, PrimerDesignService primer3) {
        this.alignmentService = alignmentService;
        this.jobs = jobs;
        this.tcoffee = tcoffee;
        this.primer3 = primer3;
    }

    @PostMapping("/api/alignments/pairwise")
    AlignmentDto createPairwiseAlignment(@Valid @RequestBody PairwiseAlignmentRequest request) {
        return alignmentService.pairwise(request.sequences(), request.mode());
    }

    @PostMapping("/api/alignments/consensus")
    SequenceDto createConsensus(@Valid @RequestBody ConsensusRequest request) {
        return alignmentService.consensus(request);
    }

    @PostMapping("/api/alignments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ToolJobDto createAlignmentJob(@Valid @RequestBody MultipleAlignmentRequest request, HttpServletRequest http) {
        return jobs.submit(request, http.getRemoteAddr());
    }

    @GetMapping("/api/tools/tcoffee")
    TcoffeeService.Capabilities tcoffeeCapabilities() {
        return tcoffee.capabilities();
    }

    @PostMapping("/api/primers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ToolJobDto createPrimerJob(@Valid @RequestBody PrimerDesignRequest request, HttpServletRequest http) {
        return jobs.submitPrimer(request, http.getRemoteAddr());
    }

    @GetMapping("/api/tools/primer3")
    PrimerDesignService.Capabilities primerCapabilities() {
        return primer3.capabilities();
    }

    @PostMapping("/api/trees")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ToolJobDto createTreeJob(@Valid @RequestBody com.genewarrior.genewarrior.api.dto.TreeRequest request, HttpServletRequest http) {
        return jobs.submitTree(request, http.getRemoteAddr());
    }

}
