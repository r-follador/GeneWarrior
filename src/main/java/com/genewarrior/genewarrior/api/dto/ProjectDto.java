package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ProjectDto(
        String id,
        @NotBlank String name,
        @Valid List<SequenceDto> sequences,
        @Valid List<AlignmentDto> alignments,
        @Valid List<SequenceLogoDto> logos,
        @Valid @JsonInclude(JsonInclude.Include.NON_EMPTY) List<PrimerDesignDto> primerDesigns,
        List<String> entryOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public ProjectDto {
        sequences = sequences == null ? List.of() : List.copyOf(sequences);
        alignments = alignments == null ? List.of() : List.copyOf(alignments);
        logos = logos == null ? List.of() : List.copyOf(logos);
        primerDesigns = primerDesigns == null ? List.of() : List.copyOf(primerDesigns);
        entryOrder = entryOrder == null ? List.of() : List.copyOf(entryOrder);
    }

    public ProjectDto(String id, String name, List<SequenceDto> sequences, List<AlignmentDto> alignments,
                      List<SequenceLogoDto> logos, List<String> entryOrder, Instant createdAt, Instant updatedAt) {
        this(id, name, sequences, alignments, logos, List.of(), entryOrder, createdAt, updatedAt);
    }
}
