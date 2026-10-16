package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record TreeRequest(
        @NotNull UUID requestId,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,160}") String sourceAlignmentId,
        @NotBlank @Size(max = 255) String name,
        @NotNull SequenceKind kind,
        @NotNull @Size(min = 3, max = 20) List<@NotNull @Valid AlignmentSequenceDto> sequences
) {
}
