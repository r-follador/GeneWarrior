package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.ToolJobDto;
import com.genewarrior.genewarrior.service.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobApiController {
    private final JobService jobService;

    public JobApiController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{id}")
    ToolJobDto get(@PathVariable String id) {
        return jobService.get(id);
    }

    @DeleteMapping("/{id}")
    ToolJobDto cancel(@PathVariable String id) {
        return jobService.cancel(id);
    }
}
