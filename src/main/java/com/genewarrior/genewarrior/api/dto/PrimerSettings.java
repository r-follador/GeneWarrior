package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PrimerSettings(
        @Min(40) @Max(20000) int productMin,
        @Min(40) @Max(20000) int productMax,
        @Min(1) @Max(10) int numReturn,
        @NotNull @Valid OligoConstraints primers,
        boolean includeProbe,
        @NotNull @Valid OligoConstraints probe,
        @Valid Region target,
        @DecimalMin("0.1") @DecimalMax("500") double saltMonovalent,
        @DecimalMin("0") @DecimalMax("100") double saltDivalent,
        @DecimalMin("0") @DecimalMax("100") double dntp,
        @DecimalMin("0.1") @DecimalMax("1000") double dnaConcentration
) {
    public record Region(@Min(0) int start, @Min(1) int end) {
    }

    public record OligoConstraints(@Min(15) @Max(35) int minSize, @Min(15) @Max(35) int optSize,
                                   @Min(15) @Max(35) int maxSize,
                                   @DecimalMin("30") @DecimalMax("90") double minTm,
                                   @DecimalMin("30") @DecimalMax("90") double optTm,
                                   @DecimalMin("30") @DecimalMax("90") double maxTm,
                                   @DecimalMin("0") @DecimalMax("100") double minGc,
                                   @DecimalMin("0") @DecimalMax("100") double maxGc) {
    }
}
