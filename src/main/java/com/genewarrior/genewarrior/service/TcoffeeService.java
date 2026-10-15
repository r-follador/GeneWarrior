package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

@Service
public class TcoffeeService {
    public static final int MAX_SEQUENCES = 20, MAX_LENGTH = 2000, MAX_RESIDUES = 10000;
    public static final String METHOD = "slow_pair,lalign_id_pair";
    private final ExternalToolRunner runner;
    private final GeneWarriorToolProperties properties;
    private volatile String version;

    public TcoffeeService(ExternalToolRunner runner, GeneWarriorToolProperties properties) {
        this.runner = runner;
        this.properties = properties;
    }

    public synchronized void probe() {
        version = null;
        if (!runner.available(properties.tcoffee().path())) return;
        try {
            var result = runner.run(List.of(properties.tcoffee().path(), "-version"), "");
            var match = Pattern.compile("Version_([0-9][A-Za-z0-9.]+)").matcher(result.stdout() + result.stderr());
            if (result.exitCode() == 0 && match.find()) version = match.group(1);
        } catch (
                RuntimeException ignored) { /* Capabilities report unavailability without exposing paths or diagnostics. */ }
    }

    public Capabilities capabilities() {
        return new Capabilities(version != null && runner.available(properties.tcoffee().path()), version,
                MAX_SEQUENCES, MAX_LENGTH, MAX_RESIDUES);
    }

    public void requireAvailable() {
        if (!capabilities().available()) throw new ExternalToolUnavailableException("T-Coffee is unavailable");
    }

    public List<SequenceDto> validate(List<SequenceDto> sequences) {
        if (sequences == null || sequences.size() < 2 || sequences.size() > MAX_SEQUENCES)
            throw new IllegalArgumentException("T-Coffee requires 2 to 20 sequences");
        List<SequenceDto> normalized = new ArrayList<>();
        int total = 0;
        for (SequenceDto sequence : sequences) {
            if (sequence == null || sequence.kind() == null || sequence.kind() != sequences.getFirst().kind())
                throw new IllegalArgumentException("T-Coffee requires sequences of the same kind");
            if (sequence.name() == null || sequence.name().isBlank() || sequence.name().length() > 255
                    || (sequence.id() != null && !sequence.id().matches("[a-zA-Z0-9_-]{1,160}")))
                throw new IllegalArgumentException("Invalid sequence name or ID");
            if (sequence.sequence() == null) throw new IllegalArgumentException("Sequence is required");
            String value = sequence.sequence().toUpperCase(Locale.ROOT);
            if (value.length() > MAX_LENGTH || (total += value.length()) > MAX_RESIDUES)
                throw new PayloadTooLargeException("T-Coffee accepts up to 2,000 residues per sequence and 10,000 in total");
            if (!value.matches(sequence.kind() == SequenceKind.DNA ? "[ACGT]+" : "[ACDEFGHIKLMNPQRSTVWY]+"))
                throw new IllegalArgumentException("T-Coffee currently requires ungapped A/C/G/T DNA or standard 20-letter proteins; ambiguity, U and stop symbols are not supported");
            normalized.add(new SequenceDto(sequence.id(), sequence.name(), sequence.kind(), value, List.of()));
        }
        return List.copyOf(normalized);
    }

    public AlignmentDto align(String jobId, List<SequenceDto> input, BooleanSupplier cancelled) {
        requireAvailable();
        List<SequenceDto> sequences = validate(input);
        StringBuilder fasta = new StringBuilder();
        for (int i = 0; i < sequences.size(); i++)
            fasta.append('>').append(identifier(i)).append('\n').append(sequences.get(i).sequence()).append('\n');
        var result = runner.run(List.of(properties.tcoffee().path(), "-seq", "input.fasta", "-type",
                        sequences.getFirst().kind() == SequenceKind.DNA ? "dna" : "protein", "-method", METHOD,
                        "-output", "fasta_aln", "-outfile", "aligned.fasta", "-n_core", "1", "-quiet"),
                "", Map.of("input.fasta", fasta.toString()), "aligned.fasta", cancelled);
        if (result.exitCode() != 0)
            throw new ToolExecutionException("TOOL_FAILED", "T-Coffee could not align these sequences");
        return parse(jobId, sequences, result.output(), version);
    }

    static AlignmentDto parse(String id, List<SequenceDto> sources, String output, String version) {
        Map<String, StringBuilder> parsed = new HashMap<>();
        String current = null;
        if (output == null) throw invalidOutput();
        for (String line : output.split("\\R")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith(">")) {
                current = line.substring(1).split("\\s+", 2)[0];
                if (parsed.putIfAbsent(current, new StringBuilder()) != null) throw invalidOutput();
            } else {
                if (current == null || !line.matches("[a-zA-Z-]+")) throw invalidOutput();
                parsed.get(current).append(line.toUpperCase(Locale.ROOT));
            }
        }
        if (parsed.size() != sources.size()) throw invalidOutput();
        List<AlignmentSequenceDto> rows = new ArrayList<>();
        List<AlignmentProvenanceDto.Source> provenance = new ArrayList<>();
        int length = -1;
        for (int i = 0; i < sources.size(); i++) {
            SequenceDto source = sources.get(i);
            StringBuilder raw = parsed.get(identifier(i));
            if (raw == null) throw invalidOutput();
            String value = raw.toString();
            if (!value.replace("-", "").equals(source.sequence()) || (length != -1 && value.length() != length))
                throw invalidOutput();
            length = value.length();
            rows.add(new AlignmentSequenceDto(source.name(), value));
            provenance.add(new AlignmentProvenanceDto.Source(source.id(), source.name(), digest(source.sequence())));
        }
        StringBuilder consensus = new StringBuilder();
        for (int column = 0; column < length; column++) {
            char residue = rows.getFirst().value().charAt(column);
            boolean same = residue != '-';
            for (var row : rows) same &= row.value().charAt(column) == residue;
            consensus.append(same ? '*' : ' ');
        }
        return new AlignmentDto("tcoffee-" + id, "T-Coffee alignment", sources.getFirst().kind(), rows,
                consensus.toString(), null, List.of(), new AlignmentProvenanceDto("T-Coffee", version, METHOD, provenance));
    }

    private static String identifier(int i) {
        return String.format(Locale.ROOT, "seq%04d", i + 1);
    }

    private static ToolExecutionException invalidOutput() {
        return new ToolExecutionException("INVALID_OUTPUT", "T-Coffee returned an invalid alignment");
    }

    private static String digest(String sequence) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sequence.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Capabilities(boolean available, String version, int maxSequences, int maxLength, int maxResidues) {
    }
}
