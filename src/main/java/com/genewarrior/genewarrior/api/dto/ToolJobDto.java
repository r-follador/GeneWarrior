package com.genewarrior.genewarrior.api.dto;

import java.time.Instant;

public record ToolJobDto(
        String id,
        String tool,
        String status,
        Instant createdAt,
        Instant finishedAt,
        Object result,
        String error,
        String errorCode,
        Instant startedAt,
        Instant expiresAt
) {
}
