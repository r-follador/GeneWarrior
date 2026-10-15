package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

@Service
public class PrimerDesignService {
    public static final int MAX_LENGTH = 20000;
    private final ExternalToolRunner runner;
    private final GeneWarriorToolProperties properties;
    private volatile String version;

    public PrimerDesignService(ExternalToolRunner runner, GeneWarriorToolProperties properties) {
        this.runner = runner;
        this.properties = properties;
    }

    public void probe() {
        version = null;
        if (!runner.available(properties.primer3().path()) || !parametersAvailable()) return;
        try {
            var response = runner.run(List.of(properties.primer3().path(), "--about"), "");
            var match = Pattern.compile("libprimer3 release ([0-9.]+)").matcher(response.stdout());
            if (response.exitCode() != 0 || !match.find()) return;
            String detected = match.group(1);
            var source = new SequenceDto("probe", "Probe", SequenceKind.DNA, "ACGT".repeat(30), List.of());
            var constraints = new PrimerSettings.OligoConstraints(18, 20, 25, 55, 60, 65, 20, 80);
            var settings = new PrimerSettings(60, 100, 1, constraints, true, constraints, null, 50, 1.5, 0.6, 50);
            // Exercise thermodynamic data as well as the binary. No acceptable pairs is a valid probe result.
            var check = runner.run(List.of(properties.primer3().path(), "--strict_tags"), boulder(source, settings));
            var tags = tags(check.stdout());
            if (check.exitCode() == 0 && !tags.containsKey("PRIMER_ERROR") && tags.containsKey("PRIMER_PAIR_NUM_RETURNED"))
                version = detected;
        } catch (
                RuntimeException ignored) { /* Report unavailability without exposing tool paths or raw diagnostics. */ }
    }

    private boolean parametersAvailable() {
        String directory = properties.primer3().parameterDirectory();
        return directory != null && !directory.isBlank() && !directory.contains("\n") && !directory.contains("\r")
                && Path.of(directory).isAbsolute() && Files.isReadable(Path.of(directory, "stack.dh"));
    }

    public Capabilities capabilities() {
        return new Capabilities(version != null && runner.available(properties.primer3().path()) && parametersAvailable(), version, MAX_LENGTH);
    }

    public void requireAvailable() {
        if (!capabilities().available())
            throw new ExternalToolUnavailableException("Primer3 or its thermodynamic parameters are unavailable");
    }

    public static SequenceDto validate(SequenceDto source, PrimerSettings settings) {
        require(source != null && source.kind() == SequenceKind.DNA && source.sequence() != null, "Primer design requires a DNA sequence");
        if (source.sequence().length() > MAX_LENGTH)
            throw new PayloadTooLargeException("Primer design accepts at most 20,000 bases");
        String sequence = source.sequence().toUpperCase(Locale.ROOT);
        require(sequence.length() >= 40 && sequence.matches("[ACGTN]+"), "Primer design requires at least 40 ungapped A/C/G/T/N bases");
        require(source.id() != null && source.id().matches("[a-zA-Z0-9_-]{1,160}"), "Invalid source ID");
        require(source.name() != null && !source.name().isBlank() && source.name().length() <= 255, "Invalid source name");
        require(settings != null, "Primer settings are required");
        constraints(settings.primers());
        constraints(settings.probe());
        require(settings.productMin() >= 40 && settings.productMin() <= settings.productMax()
                && settings.productMax() <= MAX_LENGTH && settings.productMin() <= sequence.length(), "Invalid product-size range for this template");
        require(settings.numReturn() >= 1 && settings.numReturn() <= 10, "Request 1 to 10 primer pairs");
        if (settings.target() != null) {
            var target = settings.target();
            require(target.start() >= 0 && target.end() > target.start() && target.end() <= sequence.length(), "Invalid target coordinates");
        }
        require(between(settings.saltMonovalent(), 0.1, 500) && between(settings.saltDivalent(), 0, 100)
                && between(settings.dntp(), 0, 100) && between(settings.dnaConcentration(), 0.1, 1000), "Invalid reaction concentrations");
        return new SequenceDto(source.id(), source.name(), SequenceKind.DNA, sequence, List.of());
    }

    private static void constraints(PrimerSettings.OligoConstraints c) {
        require(c != null && c.minSize() >= 15 && c.minSize() <= c.optSize() && c.optSize() <= c.maxSize() && c.maxSize() <= 35,
                "Oligo lengths must be ordered min <= optimum <= max, within 15-35 bases");
        require(between(c.minTm(), 30, 90) && between(c.optTm(), c.minTm(), 90) && between(c.maxTm(), c.optTm(), 90), "Invalid oligo temperature range");
        require(between(c.minGc(), 0, 100) && between(c.maxGc(), c.minGc(), 100), "Invalid oligo GC range");
    }

    public PrimerDesignDto design(String id, SequenceDto source, PrimerSettings settings, BooleanSupplier cancelled) {
        requireAvailable();
        source = validate(source, settings);
        var result = runner.run(List.of(properties.primer3().path(), "--strict_tags"), boulder(source, settings), Map.of(), null, cancelled);
        if (result.exitCode() != 0)
            throw new ToolExecutionException("TOOL_FAILED", "Primer3 could not complete this design");
        return parse(id, source, settings, result.stdout(), version);
    }

    String boulder(SequenceDto source, PrimerSettings s) {
        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("SEQUENCE_ID", "template");
        tags.put("SEQUENCE_TEMPLATE", source.sequence());
        tags.put("PRIMER_TASK", "generic");
        tags.put("PRIMER_FIRST_BASE_INDEX", 0);
        tags.put("PRIMER_PICK_LEFT_PRIMER", 1);
        tags.put("PRIMER_PICK_RIGHT_PRIMER", 1);
        tags.put("PRIMER_PICK_INTERNAL_OLIGO", s.includeProbe() ? 1 : 0);
        tags.put("PRIMER_NUM_RETURN", s.numReturn());
        tags.put("PRIMER_PRODUCT_SIZE_RANGE", s.productMin() + "-" + s.productMax());
        tags.put("PRIMER_MAX_NS_ACCEPTED", 0);
        tags.put("PRIMER_INTERNAL_MAX_NS_ACCEPTED", 0);
        oligoTags(tags, "PRIMER_", s.primers());
        oligoTags(tags, "PRIMER_INTERNAL_", s.probe());
        for (String prefix : List.of("PRIMER_", "PRIMER_INTERNAL_")) {
            tags.put(prefix + "SALT_MONOVALENT", s.saltMonovalent());
            tags.put(prefix + "SALT_DIVALENT", s.saltDivalent());
            tags.put(prefix + "DNTP_CONC", s.dntp());
            tags.put(prefix + "DNA_CONC", s.dnaConcentration());
        }
        if (s.target() != null)
            tags.put("SEQUENCE_TARGET", s.target().start() + "," + (s.target().end() - s.target().start()));
        tags.put("PRIMER_THERMODYNAMIC_OLIGO_ALIGNMENT", 1);
        tags.put("PRIMER_THERMODYNAMIC_TEMPLATE_ALIGNMENT", 1);
        tags.put("PRIMER_THERMODYNAMIC_PARAMETERS_PATH", Path.of(properties.primer3().parameterDirectory()) + "/");
        tags.put("PRIMER_EXPLAIN_FLAG", 1);
        StringBuilder result = new StringBuilder();
        tags.forEach((key, value) -> result.append(key).append('=').append(value).append('\n'));
        return result.append("=\n").toString();
    }

    private static void oligoTags(Map<String, Object> tags, String prefix, PrimerSettings.OligoConstraints c) {
        tags.put(prefix + "MIN_SIZE", c.minSize());
        tags.put(prefix + "OPT_SIZE", c.optSize());
        tags.put(prefix + "MAX_SIZE", c.maxSize());
        tags.put(prefix + "MIN_TM", c.minTm());
        tags.put(prefix + "OPT_TM", c.optTm());
        tags.put(prefix + "MAX_TM", c.maxTm());
        tags.put(prefix + "MIN_GC", c.minGc());
        tags.put(prefix + "MAX_GC", c.maxGc());
    }

    static PrimerDesignDto parse(String id, SequenceDto source, PrimerSettings settings, String output, String version) {
        try {
            Map<String, String> tags = tags(output);
            if (tags.containsKey("PRIMER_ERROR"))
                throw new ToolExecutionException("TOOL_FAILED", "Primer3 rejected the design settings or template");
            int count = integer(tags, "PRIMER_PAIR_NUM_RETURNED");
            require(count >= 0 && count <= settings.numReturn(), "Invalid pair count");
            List<PrimerDesignDto.Pair> pairs = new ArrayList<>();
            for (int i = 0; i < count; i++)
                pairs.add(new PrimerDesignDto.Pair(i,
                        oligo(tags, "LEFT", i), oligo(tags, "RIGHT", i), settings.includeProbe() ? oligo(tags, "INTERNAL", i) : null,
                        integer(tags, "PRIMER_PAIR_" + i + "_PRODUCT_SIZE"), number(tags, "PRIMER_PAIR_" + i + "_PENALTY")));
            List<String> explanations = new ArrayList<>();
            for (String type : List.of("LEFT", "RIGHT", "INTERNAL", "PAIR")) {
                String explanation = tags.get("PRIMER_" + type + "_EXPLAIN");
                if (explanation != null && !explanation.isBlank()) explanations.add(type + ": " + explanation);
            }
            var result = new PrimerDesignDto("primer3-" + id, source.name().substring(0, Math.min(230, source.name().length())) + " [Primers]",
                    version, source, settings, List.copyOf(pairs), List.copyOf(explanations));
            validateResult(result);
            return result;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ToolExecutionException("INVALID_OUTPUT", "Primer3 returned invalid design data");
        }
    }

    private static Map<String, String> tags(String output) {
        Map<String, String> tags = new LinkedHashMap<>();
        boolean ended = false;
        for (String line : output.split("\\R")) {
            if (line.isBlank()) continue;
            require(!ended, "Unexpected extra Boulder record");
            if (line.equals("=")) {
                ended = true;
                continue;
            }
            int separator = line.indexOf('=');
            require(separator > 0 && tags.putIfAbsent(line.substring(0, separator), line.substring(separator + 1)) == null, "Invalid Boulder output");
        }
        require(ended, "Incomplete Boulder output");
        return tags;
    }

    private static PrimerDesignDto.Oligo oligo(Map<String, String> tags, String type, int i) {
        String prefix = "PRIMER_" + type + "_" + i;
        String[] coordinates = tags.get(prefix).split(",");
        require(coordinates.length == 2, "Invalid primer coordinates");
        int position = Integer.parseInt(coordinates[0]), length = Integer.parseInt(coordinates[1]);
        boolean reverse = type.equals("RIGHT");
        return new PrimerDesignDto.Oligo(tags.get(prefix + "_SEQUENCE"), reverse ? position - length + 1 : position,
                reverse ? position + 1 : position + length, reverse ? "REVERSE" : "FORWARD",
                number(tags, prefix + "_TM"), number(tags, prefix + "_GC_PERCENT"), number(tags, prefix + "_PENALTY"));
    }

    private static int integer(Map<String, String> tags, String key) {
        return Integer.parseInt(tags.get(key));
    }

    private static double number(Map<String, String> tags, String key) {
        return Double.parseDouble(tags.get(key));
    }

    public static void validateResult(PrimerDesignDto design) {
        require(design != null, "Primer design is required");
        var source = validate(design.source(), design.settings());
        require(design.version() != null && design.version().matches("[0-9.]{1,100}"), "Invalid Primer3 version");
        require(design.pairs() != null && design.pairs().size() <= design.settings().numReturn(), "Invalid primer pairs");
        require(design.explanations() != null && design.explanations().size() <= 4 && design.explanations().stream()
                .allMatch(e -> e != null && !e.isBlank() && e.length() <= 1000), "Invalid primer explanations");
        for (int i = 0; i < design.pairs().size(); i++) {
            var pair = design.pairs().get(i);
            require(pair != null && pair.index() == i && between(pair.penalty(), 0, 1_000_000), "Invalid primer pair");
            validateOligo(source.sequence(), pair.left(), false);
            validateOligo(source.sequence(), pair.right(), true);
            require(pair.left().end() <= pair.right().start() && pair.productSize() == pair.right().end() - pair.left().start()
                    && pair.productSize() >= design.settings().productMin() && pair.productSize() <= design.settings().productMax(), "Invalid PCR product coordinates");
            var target = design.settings().target();
            require(target == null || (pair.left().end() <= target.start() && pair.right().start() >= target.end()), "Primers do not flank the target");
            require(design.settings().includeProbe() == (pair.probe() != null), "Missing or unexpected internal probe");
            if (pair.probe() != null) {
                validateOligo(source.sequence(), pair.probe(), false);
                require(pair.probe().start() >= pair.left().end() && pair.probe().end() <= pair.right().start(), "Probe is outside the primer pair");
            }
        }
    }

    private static void validateOligo(String template, PrimerDesignDto.Oligo oligo, boolean reverse) {
        require(oligo != null && oligo.start() >= 0 && oligo.end() > oligo.start() && oligo.end() <= template.length(), "Invalid oligo coordinates");
        require(oligo.sequence() != null && oligo.sequence().matches("[ACGT]{15,35}") && oligo.sequence().length() == oligo.end() - oligo.start()
                && (reverse ? "REVERSE" : "FORWARD").equals(oligo.strand()), "Invalid oligo sequence or direction");
        String expected = template.substring(oligo.start(), oligo.end());
        require(expected.matches("[ACGT]+"), "Oligo overlaps ambiguous template bases");
        if (reverse) {
            StringBuilder complement = new StringBuilder();
            for (int i = expected.length() - 1; i >= 0; i--)
                complement.append("TGCA".charAt("ACGT".indexOf(expected.charAt(i))));
            expected = complement.toString();
        }
        require(expected.equals(oligo.sequence()) && between(oligo.tm(), -100, 150) && between(oligo.gc(), 0, 100)
                && between(oligo.penalty(), 0, 1_000_000), "Oligo does not match its source or has invalid statistics");
    }

    private static boolean between(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public record Capabilities(boolean available, String version, int maxLength) {
    }
}
