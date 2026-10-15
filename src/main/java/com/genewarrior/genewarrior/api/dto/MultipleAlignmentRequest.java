package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record MultipleAlignmentRequest(@NotNull UUID requestId,
                                       @NotNull @Size(min = 2, max = 20) List<@NotNull @Valid SequenceDto> sequences) {
}
