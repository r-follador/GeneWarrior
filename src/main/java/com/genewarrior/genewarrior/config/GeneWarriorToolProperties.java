package com.genewarrior.genewarrior.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "genewarrior.tools")
public record GeneWarriorToolProperties(
        Tool tcoffee,
        Primer3 primer3,
        Duration defaultTimeout,
        int maxInputBytes,
        int maxOutputBytes,
        long maxScratchBytes
) {
    public GeneWarriorToolProperties {
        tcoffee = tcoffee == null ? new Tool("") : tcoffee;
        primer3 = primer3 == null ? new Primer3("", "") : primer3;
        defaultTimeout = defaultTimeout == null ? Duration.ofSeconds(20) : defaultTimeout;
        maxInputBytes = maxInputBytes == 0 ? 5_000_000 : maxInputBytes;
        maxOutputBytes = maxOutputBytes == 0 ? 10_000_000 : maxOutputBytes;
        maxScratchBytes = maxScratchBytes == 0 ? 32_000_000 : maxScratchBytes;
        if (defaultTimeout.isNegative() || defaultTimeout.isZero() || maxInputBytes < 1
                || maxOutputBytes < 1 || maxScratchBytes < 1) throw new IllegalArgumentException("Invalid tool limits");
    }

    public record Tool(String path) {
    }

    public record Primer3(String path, String parameterDirectory) {
    }
}
