package com.genewarrior.genewarrior.service;

import com.genewarrior.genetools.alignment.Alignment;
import com.genewarrior.genetools.alignment.AlignmentTools;
import com.genewarrior.genetools.alignment.SubstitutionMatricesBlosum2;
import com.genewarrior.genetools.alignment.SubstitutionMatricesDNA;
import com.genewarrior.genetools.alignment.SubstitutionMatrix;
import com.genewarrior.genetools.sequenceHandling.SequenceAA;
import com.genewarrior.genetools.sequenceHandling.Nucleotide;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;
import com.genewarrior.genewarrior.api.dto.AlignmentDto;
import com.genewarrior.genewarrior.api.dto.AlignmentSequenceDto;
import com.genewarrior.genewarrior.api.dto.ConsensusRequest;
import com.genewarrior.genewarrior.api.dto.PairwiseAlignmentMode;
import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AlignmentService {
    public static final int MAX_CONSENSUS_COLUMNS = 100_000;
    public static final int MAX_CONSENSUS_ROWS = 10_000;
    public static final int MAX_CONSENSUS_CELLS = 2_000_000;
    private final Nucleotide nucleotides = new Nucleotide();
    private final SubstitutionMatrix dnaMatrix = new SubstitutionMatricesDNA().dna;
    private final SubstitutionMatrix proteinMatrix = new SubstitutionMatricesBlosum2().blosum[7];

    public SequenceDto consensus(ConsensusRequest request) {
        if (request == null || request.kind() == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Consensus name and sequence type are required");
        }
        List<AlignmentSequenceDto> rows = request.sequences();
        if (rows == null || rows.isEmpty() || rows.getFirst() == null
                || rows.getFirst().value() == null || rows.getFirst().value().isEmpty()) {
            throw new IllegalArgumentException("An aligned sequence is required");
        }
        int length = rows.getFirst().value().length();
        if (length > MAX_CONSENSUS_COLUMNS || rows.size() > MAX_CONSENSUS_ROWS
                || (long) length * rows.size() > MAX_CONSENSUS_CELLS) {
            throw new PayloadTooLargeException("Consensus limit: 100,000 columns, 10,000 rows and 2,000,000 alignment cells");
        }
        for (AlignmentSequenceDto row : rows) {
            if (row == null || row.value() == null || row.value().length() != length) {
                throw new IllegalArgumentException("Alignment rows must have equal lengths");
            }
            if (!row.value().matches(request.kind() == SequenceKind.DNA ? "(?i)[ACGTURYSWKMBDHVN-]+" : "(?i)[A-Z*\\-]+")) {
                throw new IllegalArgumentException("Invalid sequence characters");
            }
        }
        StringBuilder result = new StringBuilder(length);
        int[] counts = new int[128];
        for (int position = 0; position < length; position++) {
            java.util.Arrays.fill(counts, 0);
            for (AlignmentSequenceDto row : rows) {
                char symbol = Character.toUpperCase(row.value().charAt(position));
                if (request.kind() == SequenceKind.DNA && symbol == 'U') symbol = 'T';
                counts[symbol]++;
            }
            int maximum = 0;
            int ties = 0;
            char winner = 0;
            char second = 0;
            for (char symbol = '*'; symbol <= 'Z'; symbol++) {
                if (symbol == '-' || counts[symbol] == 0) continue;
                if (counts[symbol] > maximum) {
                    maximum = counts[symbol];
                    winner = symbol;
                    ties = 1;
                } else if (counts[symbol] == maximum) {
                    second = symbol;
                    ties++;
                }
            }
            // Preserve legacy voting: a gap must strictly outnumber the winning
            // symbol to remove a column; ambiguity symbols count as literal votes.
            if (counts['-'] > maximum) continue;
            if (ties > 1) {
                winner = request.kind() == SequenceKind.PROTEIN ? 'X'
                        : ties > 2 ? 'N' : nucleotides.ambigous(winner, second);
            }
            result.append(winner);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No consensus residues remain after removing gap-dominated columns");
        }
        String name = request.name().substring(0, Math.min(request.name().length(), 243)) + " [Consensus]";
        return new SequenceDto("sequence-" + UUID.randomUUID(), name, request.kind(), result.toString(), List.of());
    }

    public AlignmentDto pairwise(List<SequenceDto> sequences, PairwiseAlignmentMode mode) {
        if (sequences.size() != 2) {
            throw new IllegalArgumentException("Pairwise alignment requires exactly two sequences");
        }
        SequenceDto first = sequences.get(0);
        SequenceDto second = sequences.get(1);
        if (first.kind() != second.kind()) {
            throw new IllegalArgumentException("Pairwise alignment requires two sequences of the same type");
        }
        if (first.kind() == SequenceKind.DNA) {
            return alignDna(first, second, mode);
        }
        return alignProtein(first, second, mode);
    }

    private AlignmentDto alignDna(SequenceDto first, SequenceDto second, PairwiseAlignmentMode mode) {
        Alignment alignment = AlignmentTools.align(
                new SequenceDNA(first.sequence(), false),
                new SequenceDNA(second.sequence(), false),
                dnaMatrix,
                alignmentType(mode)
        );
        return toDto(first, second, SequenceKind.DNA, mode, alignment,
                new SequenceDNA(alignment.a, false).toString(),
                new SequenceDNA(alignment.b, false).toString());
    }

    private AlignmentDto alignProtein(SequenceDto first, SequenceDto second, PairwiseAlignmentMode mode) {
        String firstWithoutStops = first.sequence().replace("*", "");
        String secondWithoutStops = second.sequence().replace("*", "");
        if (firstWithoutStops.isBlank() || secondWithoutStops.isBlank()) {
            throw new IllegalArgumentException("A protein sequence consists solely of stops");
        }
        Alignment alignment = AlignmentTools.align(
                new SequenceAA(firstWithoutStops),
                new SequenceAA(secondWithoutStops),
                proteinMatrix,
                alignmentType(mode)
        );
        return toDto(first, second, SequenceKind.PROTEIN, mode, alignment,
                new SequenceAA(alignment.a).toString(),
                new SequenceAA(alignment.b).toString());
    }

    private AlignmentDto toDto(SequenceDto first, SequenceDto second, SequenceKind kind, PairwiseAlignmentMode mode, Alignment alignment, String alignedFirst, String alignedSecond) {
        if (alignment == null) {
            throw new IllegalArgumentException("Could not create alignment");
        }
        return new AlignmentDto(
                "alignment-" + UUID.randomUUID(),
                "Pairwise " + (mode == PairwiseAlignmentMode.CFE ? "Full Sequence" : "Local") + " Alignment",
                kind,
                List.of(
                        new AlignmentSequenceDto(first.name(), alignedFirst),
                        new AlignmentSequenceDto(second.name(), alignedSecond)
                ),
                matchingConsensus(alignment),
                alignment.score,
                List.of()
        );
    }

    private byte alignmentType(PairwiseAlignmentMode mode) {
        return switch (mode) {
            case CFE -> AlignmentTools.CFE;
            case LOCAL -> AlignmentTools.Local;
        };
    }

    private String matchingConsensus(Alignment alignment) {
        boolean[] matches = alignment.getMatching();
        StringBuilder consensus = new StringBuilder(matches.length);
        for (boolean match : matches) {
            consensus.append(match ? '*' : ' ');
        }
        return consensus.toString();
    }
}
