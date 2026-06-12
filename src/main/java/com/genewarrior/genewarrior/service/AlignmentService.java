package com.genewarrior.genewarrior.service;

import com.genewarrior.genetools.alignment.Alignment;
import com.genewarrior.genetools.alignment.AlignmentTools;
import com.genewarrior.genetools.alignment.SubstitutionMatricesBlosum2;
import com.genewarrior.genetools.alignment.SubstitutionMatricesDNA;
import com.genewarrior.genetools.alignment.SubstitutionMatrix;
import com.genewarrior.genetools.sequenceHandling.SequenceAA;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;
import com.genewarrior.genewarrior.api.dto.AlignmentDto;
import com.genewarrior.genewarrior.api.dto.AlignmentSequenceDto;
import com.genewarrior.genewarrior.api.dto.PairwiseAlignmentMode;
import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AlignmentService {
    private final SubstitutionMatrix dnaMatrix = new SubstitutionMatricesDNA().dna;
    private final SubstitutionMatrix proteinMatrix = new SubstitutionMatricesBlosum2().blosum[7];

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
