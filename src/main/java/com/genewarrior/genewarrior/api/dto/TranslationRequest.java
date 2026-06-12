package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TranslationRequest(
        @Valid @NotNull SequenceDto sequence,
        @Min(1) @Max(6) int frame,
        @Min(1) int translationTable
) {
    public TranslationRequest {
        if (frame == 0) {
            frame = 1;
        }
        if (translationTable == 0) {
            translationTable = 1;
        }
    }
}
