package com.genewarrior.genewarrior.api.dto;

public record TranslationFramesDto(
        String sequence,
        String frame1,
        String frame2,
        String frame3,
        String markers1,
        String markers2,
        String markers3
) {
}
