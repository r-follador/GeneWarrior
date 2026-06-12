package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SaveProjectRequest(@Valid @NotNull ProjectDto project) {
}
