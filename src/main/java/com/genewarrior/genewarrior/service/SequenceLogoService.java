package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SequenceLogoService {
    public static final String METHOD = "uniform-uncorrected-v1";
    public static final int MAX_COLUMNS = 2000;
    public static final int MAX_ROWS = 10000;
    public static final int MAX_CELLS = 2_000_000;

    public static String alphabet(SequenceKind kind) {
        return kind == SequenceKind.DNA ? "ACGT" : "ACDEFGHIKLMNPQRSTVWY";
    }

    public SequenceLogoDto calculate(LogoRequest request) {
        require(request != null && request.kind() != null && request.mode() != null, "Logo kind and mode are required");
        List<AlignmentSequenceDto> rows = request.sequences();
        require(rows != null && !rows.isEmpty() && rows.getFirst() != null
                && rows.getFirst().value() != null && !rows.getFirst().value().isEmpty(), "An aligned sequence is required");
        int length = rows.getFirst().value().length();
        if (length > MAX_COLUMNS || rows.size() > MAX_ROWS || (long) length * rows.size() > MAX_CELLS) {
            throw new PayloadTooLargeException("Logo limit: 2,000 columns, 10,000 rows and 2,000,000 alignment cells");
        }
        String alphabet = alphabet(request.kind());
        int[][] counts = new int[length][alphabet.length()];
        int[] gaps = new int[length];
        int[] ignored = new int[length];
        for (AlignmentSequenceDto row : rows) {
            require(row != null && row.value() != null && row.value().length() == length, "Alignment rows must have equal lengths");
            require(row.value().matches(request.kind() == SequenceKind.DNA ? "(?i)[ACGTURYSWKMBDHVN-]+" : "(?i)[A-Z*\\-]+"), "Invalid sequence characters");
            for (int position = 0; position < length; position++) {
                char residue = Character.toUpperCase(row.value().charAt(position));
                if (request.kind() == SequenceKind.DNA && residue == 'U') residue = 'T';
                int index = alphabet.indexOf(residue);
                if (index >= 0) counts[position][index]++;
                else if (residue == '-') gaps[position]++;
                else ignored[position]++;
            }
        }
        List<SequenceLogoDto.Column> columns = new ArrayList<>();
        for (int position = 0; position < length; position++) {
            columns.add(column(position + 1, alphabet, counts[position], gaps[position], ignored[position]));
        }
        return new SequenceLogoDto("logo-" + UUID.randomUUID(), request.name(), request.sourceAlignmentId(),
                request.kind(), request.mode(), METHOD, rows.size(), List.copyOf(columns));
    }

    private static SequenceLogoDto.Column column(int position, String alphabet, int[] counts, int gaps, int ignored) {
        int n = java.util.Arrays.stream(counts).sum();
        double information = 0;
        if (n > 0) {
            information = Math.log(alphabet.length()) / Math.log(2);
            for (int count : counts) {
                if (count > 0) {
                    double p = (double) count / n;
                    information += p * Math.log(p) / Math.log(2);
                }
            }
            information = Math.max(0, information);
        }
        List<SequenceLogoDto.Symbol> symbols = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            double p = n == 0 ? 0 : (double) counts[i] / n;
            symbols.add(new SequenceLogoDto.Symbol(alphabet.substring(i, i + 1), counts[i], p, p * information));
        }
        return new SequenceLogoDto.Column(position, n, gaps, ignored, information, List.copyOf(symbols));
    }

    public static void validate(SequenceLogoDto logo) {
        require(logo != null && logo.kind() != null && logo.mode() != null && METHOD.equals(logo.method()), "Invalid logo method");
        require(logo.sequenceCount() > 0 && logo.sequenceCount() <= MAX_ROWS, "Invalid logo sequence count");
        require(logo.sourceAlignmentId() != null && logo.sourceAlignmentId().matches("[a-zA-Z0-9_-]{1,160}"), "Invalid logo source ID");
        require(logo.columns() != null && !logo.columns().isEmpty() && logo.columns().size() <= MAX_COLUMNS
                && (long) logo.columns().size() * logo.sequenceCount() <= MAX_CELLS, "Invalid logo dimensions");
        String alphabet = alphabet(logo.kind());
        for (int i = 0; i < logo.columns().size(); i++) {
            var c = logo.columns().get(i);
            require(c != null && c.symbols() != null && c.symbols().size() == alphabet.length(), "Invalid logo column");
            int[] counts = new int[alphabet.length()];
            for (int j = 0; j < alphabet.length(); j++) {
                var symbol = c.symbols().get(j);
                require(symbol != null && alphabet.substring(j, j + 1).equals(symbol.residue())
                        && symbol.count() >= 0 && symbol.count() <= logo.sequenceCount(), "Invalid logo counts");
                counts[j] = symbol.count();
            }
            var expected = column(i + 1, alphabet, counts, c.gaps(), c.ignored());
            require(c.position() == i + 1 && c.observations() == expected.observations()
                    && c.gaps() >= 0 && c.ignored() >= 0
                    && (long) c.observations() + c.gaps() + c.ignored() == logo.sequenceCount()
                    && near(c.information(), expected.information()), "Invalid logo statistics");
            for (int j = 0; j < alphabet.length(); j++) {
                require(near(c.symbols().get(j).frequency(), expected.symbols().get(j).frequency())
                        && near(c.symbols().get(j).bits(), expected.symbols().get(j).bits()), "Invalid logo heights");
            }
        }
    }

    private static boolean near(double actual, double expected) {
        return Double.isFinite(actual) && actual >= 0 && Math.abs(actual - expected) < 1e-9;
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
