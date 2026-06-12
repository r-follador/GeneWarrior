package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SequenceDto(
        String id,
        @NotBlank String name,
        @NotNull SequenceKind kind,
        @NotBlank String sequence,
        @Valid List<SequenceMarkDto> marks
) {
    public SequenceDto {
        marks = marks == null ? List.of() : List.copyOf(marks);
    }
}
