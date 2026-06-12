package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public record ProjectDto(
        String id,
        @NotBlank String name,
        @Valid List<SequenceDto> sequences,
        Instant createdAt,
        Instant updatedAt
) {
    public ProjectDto {
        sequences = sequences == null ? List.of() : List.copyOf(sequences);
    }
}
