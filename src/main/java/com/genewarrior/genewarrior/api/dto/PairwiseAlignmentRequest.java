package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PairwiseAlignmentRequest(
        @Valid @Size(min = 2, max = 2) List<SequenceDto> sequences,
        @NotNull PairwiseAlignmentMode mode
) {
    public PairwiseAlignmentRequest {
        sequences = sequences == null ? List.of() : List.copyOf(sequences);
    }
}
