package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record LogoRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,160}") String sourceAlignmentId,
        @NotBlank @Size(max = 255) String name,
        @NotNull SequenceKind kind,
        @NotNull LogoMode mode,
        @NotEmpty List<@NotNull @Valid AlignmentSequenceDto> sequences
) {
}
