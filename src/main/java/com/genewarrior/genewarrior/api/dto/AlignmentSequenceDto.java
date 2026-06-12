package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AlignmentSequenceDto(
        @NotBlank String name,
        @NotBlank String value
) {
}
