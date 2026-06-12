package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TranslationRangeRequest(
        @Valid @NotNull SequenceDto sequence,
        @Min(0) int start,
        @Min(0) int end,
        @Min(1) int translationTable
) {
    public TranslationRangeRequest {
        if (translationTable == 0) {
            translationTable = 1;
        }
    }
}
