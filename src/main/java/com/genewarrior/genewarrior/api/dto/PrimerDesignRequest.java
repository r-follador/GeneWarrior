package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PrimerDesignRequest(@NotNull UUID requestId, @NotNull @Valid SequenceDto sequence,
                                  @NotNull @Valid PrimerSettings settings) {
}
