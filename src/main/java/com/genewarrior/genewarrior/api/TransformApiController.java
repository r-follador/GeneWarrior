package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.TransformRequest;
import com.genewarrior.genewarrior.service.SequenceTransformService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transforms")
public class TransformApiController {
    private final SequenceTransformService sequenceTransformService;

    public TransformApiController(SequenceTransformService sequenceTransformService) {
        this.sequenceTransformService = sequenceTransformService;
    }

    @PostMapping
    SequenceDto transform(@Valid @RequestBody TransformRequest request) {
        return sequenceTransformService.transform(request.sequence(), request.transform());
    }
}
