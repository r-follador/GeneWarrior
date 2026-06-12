package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TransformRequest(
        @Valid @NotNull SequenceDto sequence,
        @NotNull TransformType transform
) {
}
