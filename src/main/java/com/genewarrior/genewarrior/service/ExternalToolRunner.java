package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

@Service
public class ExternalToolRunner {
    private static final Logger log = LoggerFactory.getLogger(ExternalToolRunner.class);
    private final GeneWarriorToolProperties properties;

    public ExternalToolRunner(GeneWarriorToolProperties properties) {
        this.properties = properties;
    }

    public boolean available(String executable) {
        return executable != null && !executable.isBlank() && Path.of(executable).isAbsolute()
                && Files.isRegularFile(Path.of(executable)) && Files.isExecutable(Path.of(executable));
    }

    public ToolRunResult run(List<String> command, String stdin) {
        return run(command, stdin, Map.of(), null, () -> false);
    }

    // Commands and filenames are constructed by tool services, never supplied by API clients.
    public ToolRunResult run(List<String> command, String stdin, Map<String, String> files,
                             String outputFile, BooleanSupplier cancelled) {
        if (command.isEmpty() || !available(command.getFirst()))
            throw new ExternalToolUnavailableException("External tool is not available");
        long deadline = System.nanoTime() + properties.defaultTimeout().toNanos();
        byte[] input = (stdin == null ? "" : stdin).getBytes(StandardCharsets.UTF_8);
        long inputSize = input.length + files.values().stream().mapToLong(s -> s.getBytes(StandardCharsets.UTF_8).length).sum();
        if (inputSize > properties.maxInputBytes())
            throw new PayloadTooLargeException("External tool input is too large");
        Path directory = null;
        FileChannel scratchLock = null;
        Process process = null;
        Set<ProcessHandle> descendants = new HashSet<>();
        ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
        try {
            directory = Files.createTempDirectory("genewarrior-tool-");
            scratchLock = FileChannel.open(directory.resolve(".lock"), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            scratchLock.lock();
            for (var file : files.entrySet()) Files.writeString(child(directory, file.getKey()), file.getValue());
            ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
            builder.environment().clear();
            builder.environment().putAll(Map.of("PATH", "/usr/bin:/bin", "LC_ALL", "C", "HOME", directory.toString(),
                    "TMPDIR", directory.toString(), "TMP_4_TCOFFEE", directory.toString(), "OMP_NUM_THREADS", "1"));
            process = builder.start();
            Process running = process;
            AtomicLong outputSize = new AtomicLong();
            Future<String> stdout = io.submit(() -> read(running.getInputStream(), outputSize));
            Future<String> stderr = io.submit(() -> read(running.getErrorStream(), outputSize));
            Future<?> writer = io.submit(() -> {
                try (var stream = running.getOutputStream()) {
                    stream.write(input);
                }
                return null;
            });
            while (true) {
                running.descendants().forEach(descendants::add);
                if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean())
                    throw new ToolExecutionException("CANCELLED", "Job was cancelled");
                if (System.nanoTime() >= deadline)
                    throw new ToolExecutionException("TIMEOUT", "External tool timed out");
                checkScratch(directory);
                checkFailure(stdout);
                checkFailure(stderr);
                checkFailure(writer);
                if (!running.isAlive() && stdout.isDone() && stderr.isDone() && writer.isDone()) break;
                Thread.sleep(50);
            }
            String output = null;
            if (outputFile != null && running.exitValue() == 0) {
                Path path = child(directory, outputFile);
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > properties.maxOutputBytes())
                    throw new ToolExecutionException("INVALID_OUTPUT", "External tool produced invalid output");
                try (var stream = Files.newInputStream(path)) {
                    output = read(stream, outputSize);
                }
            }
            return new ToolRunResult(running.exitValue(), stdout.get(), stderr.get(), output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolExecutionException("INTERRUPTED", "Tool execution was interrupted");
        } catch (IOException | ExecutionException e) {
            throw new ToolExecutionException("EXECUTION_FAILED", "External tool execution failed");
        } finally {
            if (process != null) terminate(process, descendants);
            io.shutdownNow();
            if (directory != null) cleanup(directory);
            if (scratchLock != null) try {
                scratchLock.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String read(InputStream stream, AtomicLong total) throws IOException {
        try (stream; var bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            for (int n; (n = stream.read(buffer)) != -1; ) {
                if (total.addAndGet(n) > properties.maxOutputBytes())
                    throw new ToolExecutionException("OUTPUT_LIMIT", "External tool output limit exceeded");
                bytes.write(buffer, 0, n);
            }
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }

    private void checkFailure(Future<?> future) throws ExecutionException, InterruptedException {
        if (future.isDone()) {
            try {
                future.get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ToolExecutionException failure) throw failure;
                throw e;
            }
        }
    }

    private void checkScratch(Path directory) throws IOException {
        long total = 0;
        int count = 0;
        try (var paths = Files.walk(directory)) {
            for (var iterator = paths.iterator(); iterator.hasNext(); ) {
                Path path = iterator.next();
                if (Files.isSymbolicLink(path))
                    throw new ToolExecutionException("INVALID_OUTPUT", "Unexpected tool symlink");
                try {
                    if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) total += Files.size(path);
                } catch (NoSuchFileException ignored) { /* Tools may remove temporary files during the scan. */ }
                if (++count > 2000 || total > properties.maxScratchBytes())
                    throw new ToolExecutionException("SCRATCH_LIMIT", "External tool scratch limit exceeded");
            }
        } catch (UncheckedIOException e) {
            if (!(e.getCause() instanceof NoSuchFileException)) throw e;
        }
    }

    private static Path child(Path directory, String name) {
        if (!name.matches("[a-zA-Z0-9_.-]+") || name.equals(".") || name.equals(".."))
            throw new IllegalArgumentException("Invalid tool filename");
        return directory.resolve(name);
    }

    private static void terminate(Process process, Set<ProcessHandle> descendants) {
        boolean interrupted = Thread.interrupted();
        process.descendants().forEach(descendants::add);
        descendants.forEach(ProcessHandle::destroyForcibly);
        // Let a waiting parent reap children before killing it; all waits share a short cleanup budget.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        for (ProcessHandle child : descendants) {
            try {
                child.onExit().get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            } catch (ExecutionException | TimeoutException ignored) {
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        process.destroyForcibly();
        try {
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            interrupted = true;
        }
        try {
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private static void cleanup(Path directory) {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not fully remove external tool scratch directory");
        }
    }

    @PostConstruct
    public void cleanupAbandonedWorkspaces() {
        long cutoff = System.currentTimeMillis() - properties.defaultTimeout().plusSeconds(60).toMillis();
        try (var directories = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            for (Path directory : directories.filter(path -> path.getFileName().toString().startsWith("genewarrior-tool-")).toList()) {
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                        || Files.getLastModifiedTime(directory).toMillis() > cutoff
                        || !Files.getOwner(directory).getName().equals(System.getProperty("user.name"))) continue;
                Path lock = directory.resolve(".lock");
                if (!Files.exists(lock, LinkOption.NOFOLLOW_LINKS)) {
                    cleanup(directory);
                    continue;
                }
                if (!Files.isRegularFile(lock, LinkOption.NOFOLLOW_LINKS)) continue;
                try (var channel = FileChannel.open(lock, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                    try (var acquired = channel.tryLock()) {
                        if (acquired != null) cleanup(directory);
                    }
                } catch (OverlappingFileLockException ignored) { /* Another runner in this JVM owns it. */ }
            }
        } catch (IOException e) {
            log.warn("Could not inspect abandoned tool workspaces");
        }
    }

    public void checkConfiguredTools() {
        log.info("Configured executables: T-Coffee={}, Primer3={}", available(properties.tcoffee().path()), available(properties.primer3().path()));
    }

    public record ToolRunResult(int exitCode, String stdout, String stderr, String output) {
    }
}
