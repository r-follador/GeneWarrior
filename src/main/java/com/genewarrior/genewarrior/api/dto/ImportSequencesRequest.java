package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportSequencesRequest(
        @NotBlank @Size(max = 5_000_000) String text
) {
}
