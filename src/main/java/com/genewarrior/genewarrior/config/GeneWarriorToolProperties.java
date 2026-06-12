package com.genewarrior.genewarrior.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "genewarrior.tools")
public record GeneWarriorToolProperties(
        Tool muscle,
        Tool primer3,
        Tool weblogo,
        Duration defaultTimeout,
        int maxInputBytes,
        int maxOutputBytes
) {
    public GeneWarriorToolProperties {
        muscle = muscle == null ? new Tool("") : muscle;
        primer3 = primer3 == null ? new Tool("") : primer3;
        weblogo = weblogo == null ? new Tool("") : weblogo;
        defaultTimeout = defaultTimeout == null ? Duration.ofSeconds(20) : defaultTimeout;
        maxInputBytes = maxInputBytes == 0 ? 5_000_000 : maxInputBytes;
        maxOutputBytes = maxOutputBytes == 0 ? 10_000_000 : maxOutputBytes;
    }

    public record Tool(String path) {
    }
}
