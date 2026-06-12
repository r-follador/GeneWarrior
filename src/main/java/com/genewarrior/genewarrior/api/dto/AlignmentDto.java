package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AlignmentDto(
        @NotBlank String id,
        @NotBlank String name,
        @NotNull SequenceKind kind,
        @Valid @NotEmpty List<AlignmentSequenceDto> sequences,
        @NotNull String consensus,
        int score,
        @Valid List<SequenceMarkDto> marks
) {
    public AlignmentDto {
        sequences = List.copyOf(sequences);
        marks = marks == null ? List.of() : List.copyOf(marks);
    }
}
