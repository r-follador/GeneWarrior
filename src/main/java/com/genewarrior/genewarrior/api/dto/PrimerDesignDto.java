package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record PrimerDesignDto(@NotBlank @Size(max = 160) String id, @NotBlank @Size(max = 255) String name,
                              @NotBlank @Size(max = 100) String version, @NotNull @Valid SequenceDto source,
                              @NotNull @Valid PrimerSettings settings,
                              @NotNull @Size(max = 10) List<@NotNull @Valid Pair> pairs,
                              @NotNull @Size(max = 4) List<@NotBlank @Size(max = 1000) String> explanations) {
    public record Oligo(@NotBlank @Pattern(regexp = "[ACGT]+") String sequence,
                        @Min(0) int start, @Min(1) int end, @NotNull @Pattern(regexp = "FORWARD|REVERSE") String strand,
                        double tm, double gc, double penalty) {
    }

    public record Pair(@Min(0) int index, @NotNull @Valid Oligo left, @NotNull @Valid Oligo right,
                       @Valid Oligo probe, @Min(1) int productSize, double penalty) {
    }
}
