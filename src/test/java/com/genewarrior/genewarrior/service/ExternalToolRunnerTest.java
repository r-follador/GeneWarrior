package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExternalToolRunnerTest {
    private ExternalToolRunner runner(int output, long scratch, Duration timeout) {
        return new ExternalToolRunner(new GeneWarriorToolProperties(null, null, timeout, 1_000_000, output, scratch));
    }

    private ExternalToolRunner runner() {
        return runner(1_000_000, 1_000_000, Duration.ofSeconds(3));
    }

    @Test
    void drainsBothPipesBeforeWaitingAndWritesStdinConcurrently() {
        var result = runner().run(List.of("/bin/sh", "-c",
                "i=0; while [ $i -lt 10000 ]; do echo abcdefghijklmnop; echo abcdefghijklmnop >&2; i=$((i+1)); done; cat"), "input");
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().endsWith("input"));
        assertTrue(result.stderr().length() > 65536);
    }

    @Test
    void deadlineIncludesBlockedStdinAndKillsDescendants() {
        long start = System.nanoTime();
        var error = assertThrows(ToolExecutionException.class, () -> runner(10000, 10000, Duration.ofMillis(150))
                .run(List.of("/bin/sh", "-c", "sleep 30 & wait"), "A".repeat(500000)));
        assertEquals("TIMEOUT", error.code());
        assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 5);
    }

    @Test
    void enforcesStreamAndScratchLimits() {
        assertEquals("OUTPUT_LIMIT", assertThrows(ToolExecutionException.class,
                () -> runner(100, 10000, Duration.ofSeconds(2)).run(List.of("/bin/sh", "-c", "yes A"), "")).code());
        assertEquals("SCRATCH_LIMIT", assertThrows(ToolExecutionException.class,
                () -> runner(10000, 10, Duration.ofSeconds(2)).run(List.of("/bin/sh", "-c", "printf 12345678901234567890 > big; sleep 1"), "")).code());
    }

    @Test
    void cancellationAndFailedStartAreBounded() {
        assertEquals("CANCELLED", assertThrows(ToolExecutionException.class,
                () -> runner().run(List.of("/bin/sleep", "30"), "", Map.of(), null, () -> true)).code());
        assertThrows(ExternalToolUnavailableException.class, () -> runner().run(List.of("/not/an/executable"), ""));
    }

    @Test
    void isolatesEnvironmentReadsOutputAndCleansWorkDirectory() {
        var result = runner().run(List.of("/bin/sh", "-c", "pwd; env >&2; cat input > output"), "",
                Map.of("input", "ACGT"), "output", () -> false);
        assertEquals("ACGT", result.output());
        assertFalse(java.nio.file.Files.exists(java.nio.file.Path.of(result.stdout().trim())));
        assertFalse(result.stderr().contains("DB_PASSWORD"));
        assertFalse(result.stderr().contains("JAVA_TOOL_OPTIONS"));
        assertTrue(result.stderr().contains("TMP_4_TCOFFEE="));
    }

    @Test
    void rejectsSymlinksAndOversizedInputs() {
        assertEquals("INVALID_OUTPUT", assertThrows(ToolExecutionException.class,
                () -> runner().run(List.of("/bin/sh", "-c", "ln -s /etc/passwd output; sleep 1"), "", Map.of(), "output", () -> false)).code());
        assertThrows(PayloadTooLargeException.class, () -> runner().run(List.of("/bin/cat"), "A".repeat(1_000_001)));
    }

    @Test
    void startupCleansAbandonedScratchButNotLockedWorkspaces() throws Exception {
        var directory = java.nio.file.Files.createTempDirectory("genewarrior-tool-");
        var lock = directory.resolve(".lock");
        try (var channel = java.nio.channels.FileChannel.open(lock, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
            java.nio.file.Files.setLastModifiedTime(directory, java.nio.file.attribute.FileTime.fromMillis(0));
            try (var active = channel.lock()) {
                runner().cleanupAbandonedWorkspaces();
                assertTrue(java.nio.file.Files.exists(directory));
            }
            runner().cleanupAbandonedWorkspaces();
            assertFalse(java.nio.file.Files.exists(directory));
        } finally {
            java.nio.file.Files.deleteIfExists(lock);
            java.nio.file.Files.deleteIfExists(directory);
        }
    }
}
