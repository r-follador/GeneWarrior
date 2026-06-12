package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ExternalToolRunner {
    private static final Logger log = LoggerFactory.getLogger(ExternalToolRunner.class);

    private final GeneWarriorToolProperties properties;

    public ExternalToolRunner(GeneWarriorToolProperties properties) {
        this.properties = properties;
    }

    public ToolRunResult run(List<String> command, String stdin) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Command is required");
        }
        ensureExecutable(command.getFirst());
        byte[] input = stdin == null ? new byte[0] : stdin.getBytes(StandardCharsets.UTF_8);
        if (input.length > properties.maxInputBytes()) {
            throw new IllegalArgumentException("External tool input is too large");
        }
        try {
            Process process = new ProcessBuilder(command).start();
            process.getOutputStream().write(input);
            process.getOutputStream().close();
            Duration timeout = properties.defaultTimeout();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ExternalToolUnavailableException("External tool timed out");
            }
            byte[] stdout = process.getInputStream().readNBytes(properties.maxOutputBytes() + 1);
            byte[] stderr = process.getErrorStream().readNBytes(properties.maxOutputBytes() + 1);
            if (stdout.length > properties.maxOutputBytes() || stderr.length > properties.maxOutputBytes()) {
                throw new IllegalArgumentException("External tool output is too large");
            }
            return new ToolRunResult(process.exitValue(), new String(stdout, StandardCharsets.UTF_8), new String(stderr, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ExternalToolUnavailableException("External tool could not be started");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalToolUnavailableException("External tool run was interrupted");
        }
    }

    public void checkConfiguredTools() {
        check("muscle", properties.muscle().path());
        check("primer3", properties.primer3().path());
        check("weblogo", properties.weblogo().path());
    }

    private void check(String name, String path) {
        if (path == null || path.isBlank()) {
            log.info("{} executable is not configured", name);
            return;
        }
        if (!Files.isExecutable(Path.of(path))) {
            log.warn("{} executable is not available at {}", name, path);
        }
    }

    private void ensureExecutable(String path) {
        if (!Files.isExecutable(Path.of(path))) {
            throw new ExternalToolUnavailableException("External tool is not available: " + path);
        }
    }

    public record ToolRunResult(int exitCode, String stdout, String stderr) {
    }
}
