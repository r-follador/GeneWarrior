package com.genewarrior.genewarrior.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("genewarrior.jobs")
public record JobProperties(int queueCapacity, Duration retention, int submissionsPerMinute) {
    public JobProperties {
        queueCapacity = queueCapacity == 0 ? 8 : queueCapacity;
        retention = retention == null ? Duration.ofDays(1) : retention;
        submissionsPerMinute = submissionsPerMinute == 0 ? 10 : submissionsPerMinute;
        if (queueCapacity < 1 || submissionsPerMinute < 1 || retention.compareTo(Duration.ofMinutes(10)) < 0)
            throw new IllegalArgumentException("Invalid job limits");
    }
}
