package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record AlignmentProvenanceDto(@NotBlank @Size(max = 100) String tool,
                                     @NotBlank @Size(max = 100) String version,
                                     @NotBlank @Size(max = 100) String method,
                                     @NotNull @Size(min = 2, max = 20) List<@NotNull @Valid Source> sources) {
    public record Source(@Size(max = 160) String id, @NotBlank @Size(max = 255) String name,
                         @NotNull @Pattern(regexp = "[a-f0-9]{64}") String sha256) {
    }
}
