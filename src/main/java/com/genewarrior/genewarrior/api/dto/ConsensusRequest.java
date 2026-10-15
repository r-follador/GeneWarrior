package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConsensusRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull SequenceKind kind,
        @NotEmpty List<@NotNull @Valid AlignmentSequenceDto> sequences
) {
}
