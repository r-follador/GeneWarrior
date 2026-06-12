package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SequenceMarkDto(
        @Min(0) int start,
        @Min(0) int end,
        @NotBlank String color,
        String annotation,
        String type,
        String direction
) {
    public SequenceMarkDto(int start, int end, String color, String annotation) {
        this(start, end, color, annotation, null, null);
    }
}
