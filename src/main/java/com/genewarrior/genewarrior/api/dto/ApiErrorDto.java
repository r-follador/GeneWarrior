package com.genewarrior.genewarrior.api.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fields
) {
}
