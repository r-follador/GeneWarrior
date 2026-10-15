package com.genewarrior.genewarrior.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
        Integer score,
        @Valid List<SequenceMarkDto> marks,
        @Valid @JsonInclude(JsonInclude.Include.NON_NULL) AlignmentProvenanceDto provenance
) {
    public AlignmentDto {
        sequences = List.copyOf(sequences);
        marks = marks == null ? List.of() : List.copyOf(marks);
    }

    public AlignmentDto(String id, String name, SequenceKind kind, List<AlignmentSequenceDto> sequences,
                        String consensus, Integer score, List<SequenceMarkDto> marks) {
        this(id, name, kind, sequences, consensus, score, marks, null);
    }
}
