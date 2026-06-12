package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.ImportSequencesRequest;
import com.genewarrior.genewarrior.api.dto.ImportSequencesResponse;
import com.genewarrior.genewarrior.service.SequenceImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sequences")
public class SequenceApiController {
    private final SequenceImportService sequenceImportService;

    public SequenceApiController(SequenceImportService sequenceImportService) {
        this.sequenceImportService = sequenceImportService;
    }

    @PostMapping("/import")
    ImportSequencesResponse importSequences(@Valid @RequestBody ImportSequencesRequest request) {
        return new ImportSequencesResponse(sequenceImportService.importText(request.text()));
    }
}
