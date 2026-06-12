package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.ProjectDto;
import com.genewarrior.genewarrior.api.dto.SaveProjectRequest;
import com.genewarrior.genewarrior.api.dto.SaveProjectResponse;
import com.genewarrior.genewarrior.service.ProjectSharingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {
    private final ProjectSharingService projectSharingService;

    public ProjectApiController(ProjectSharingService projectSharingService) {
        this.projectSharingService = projectSharingService;
    }

    @PostMapping
    SaveProjectResponse save(@Valid @RequestBody SaveProjectRequest request) {
        return projectSharingService.save(request.project());
    }

    @GetMapping("/{shareKey}")
    ProjectDto load(@PathVariable String shareKey) {
        return projectSharingService.load(shareKey);
    }
}
