package com.genewarrior.genewarrior.api;

import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.TranslationFramesDto;
import com.genewarrior.genewarrior.api.dto.TranslationRangeRequest;
import com.genewarrior.genewarrior.api.dto.TranslationRequest;
import com.genewarrior.genewarrior.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/translations")
public class TranslationApiController {
    private final TranslationService translationService;

    public TranslationApiController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping
    SequenceDto translate(@Valid @RequestBody TranslationRequest request) {
        return translationService.translate(request.sequence(), request.frame(), request.translationTable());
    }

    @PostMapping("/best-orf")
    SequenceDto translateBestOrf(@Valid @RequestBody TranslationRequest request) {
        return translationService.translateBestOrf(request.sequence(), request.translationTable());
    }

    @PostMapping("/frames")
    TranslationFramesDto frames(@Valid @RequestBody TranslationRequest request) {
        return translationService.frames(request.sequence(), request.translationTable());
    }

    @PostMapping("/range")
    SequenceDto translateRange(@Valid @RequestBody TranslationRangeRequest request) {
        return translationService.translateRange(request.sequence(), request.start(), request.end(), request.translationTable());
    }
}
