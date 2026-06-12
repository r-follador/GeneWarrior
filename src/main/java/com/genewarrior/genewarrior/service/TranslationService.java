package com.genewarrior.genewarrior.service;

import com.genewarrior.genetools.sequenceHandling.SequenceAA;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;
import com.genewarrior.genetools.sequenceHandling.SequenceTools;
import com.genewarrior.genetools.sequenceHandling.Position;
import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import com.genewarrior.genewarrior.api.dto.TranslationFramesDto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TranslationService {
    private static final Set<Integer> SUPPORTED_TRANSLATION_TABLES = Set.of(1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14, 15, 16, 21, 22, 23, 24);

    public SequenceDto translate(SequenceDto sequence, int frame, int translationTable) {
        if (sequence.kind() != SequenceKind.DNA) {
            throw new IllegalArgumentException("Only DNA sequences can be translated");
        }
        validateTranslationTable(translationTable);
        boolean reverse = frame > 3;
        int localFrame = reverse ? frame - 3 : frame;
        SequenceDNA dna = new SequenceDNA(sequence.sequence(), false);
        if (reverse) {
            dna = SequenceTools.reverseComplement(dna, true, true);
        }
        if (dna.getLength() < localFrame + 2) {
            throw new IllegalArgumentException("Sequence is too short to translate frame " + frame);
        }
        SequenceAA translated = SequenceTools.DNA2Protein(dna, translationTable, localFrame, false);
        String frameLabel = (reverse ? "Reverse " : "Forward ") + "Frame " + localFrame;
        return new SequenceDto(UUID.randomUUID().toString(), sequence.name() + " [Translated " + frameLabel + "]", SequenceKind.PROTEIN, translated.toString(), List.of());
    }

    public SequenceDto translateBestOrf(SequenceDto sequence, int translationTable) {
        if (sequence.kind() != SequenceKind.DNA) {
            throw new IllegalArgumentException("Only DNA sequences can be translated");
        }
        validateTranslationTable(translationTable);
        SequenceDNA forwardDna = new SequenceDNA(sequence.sequence(), false);
        SequenceDNA reverseDna = SequenceTools.reverseComplement(forwardDna, true, true);
        Orf forward = findLongestOrf(forwardDna, translationTable);
        Orf reverse = findLongestOrf(reverseDna, translationTable);
        if (forward == null && reverse == null) {
            throw new IllegalArgumentException(sequence.name() + ": No ORF found");
        }
        boolean isReverse = forward == null || (reverse != null && reverse.length() > forward.length());
        Orf orf = isReverse ? reverse : forward;
        SequenceDNA source = isReverse ? reverseDna : forwardDna;
        SequenceAA translated = SequenceTools.DNA2Protein(source.getLinearSubSequence(new Position(orf.start(), orf.end(), false)), translationTable, 1);
        String name = sequence.name() + " [Longest ORF: " + (isReverse ? "RevComp " : "") + (orf.start() + 1) + "-" + (orf.end() + 1) + "nt]";
        return new SequenceDto(UUID.randomUUID().toString(), name, SequenceKind.PROTEIN, translated.toString(), List.of());
    }

    public TranslationFramesDto frames(SequenceDto sequence, int translationTable) {
        if (sequence.kind() != SequenceKind.DNA) {
            throw new IllegalArgumentException("Only DNA sequences can be translated");
        }
        validateTranslationTable(translationTable);
        SequenceDNA dna = new SequenceDNA(sequence.sequence(), false);
        SequenceAA frame1 = translateFrameOrDash(dna, translationTable, 1);
        SequenceAA frame2 = translateFrameOrDash(dna, translationTable, 2);
        SequenceAA frame3 = translateFrameOrDash(dna, translationTable, 3);
        char[][] markers = {
                markerArray(frame1.getLength()),
                markerArray(frame2.getLength()),
                markerArray(frame3.getLength())
        };
        int[] starts = SequenceTools.findStartCodon(dna, translationTable);
        int[] stops = SequenceTools.findTerminatorCodon(dna, translationTable);
        if (starts != null) {
            for (int start : starts) {
                int frame = start % 3;
                markers[frame][(start - frame) / 3] = 'a';
            }
        }
        if (stops != null) {
            for (int stop : stops) {
                int frame = stop % 3;
                markers[frame][(stop - frame) / 3] = 'b';
            }
        }
        return new TranslationFramesDto(dna.toString(), frame1.toString(), frame2.toString(), frame3.toString(), new String(markers[0]), new String(markers[1]), new String(markers[2]));
    }

    public SequenceDto translateRange(SequenceDto sequence, int start, int end, int translationTable) {
        if (sequence.kind() != SequenceKind.DNA) {
            throw new IllegalArgumentException("Only DNA sequences can be translated");
        }
        validateTranslationTable(translationTable);
        if (start < 0 || end < start || end >= sequence.sequence().length()) {
            throw new IllegalArgumentException("Invalid translation range");
        }
        SequenceDNA dna = new SequenceDNA(sequence.sequence(), false).getLinearSubSequence(new Position(start, end, false));
        if (dna.getLength() < 3) {
            throw new IllegalArgumentException("Sequence is too short to translate selected range");
        }
        SequenceAA translated = SequenceTools.DNA2Protein(dna, translationTable, 1, false);
        return new SequenceDto(UUID.randomUUID().toString(), sequence.name() + " [Translated " + (start + 1) + "-" + (end + 1) + "]", SequenceKind.PROTEIN, translated.toString(), List.of());
    }

    private void validateTranslationTable(int translationTable) {
        if (!SUPPORTED_TRANSLATION_TABLES.contains(translationTable)) {
            throw new IllegalArgumentException("Unsupported translation table " + translationTable);
        }
    }

    private SequenceAA translateFrameOrDash(SequenceDNA dna, int translationTable, int frame) {
        SequenceAA translated = SequenceTools.DNA2Protein(dna, translationTable, frame, false);
        return translated == null ? new SequenceAA("-") : translated;
    }

    private char[] markerArray(int length) {
        char[] markers = new char[length];
        Arrays.fill(markers, 'o');
        return markers;
    }

    private Orf findLongestOrf(SequenceDNA sequence, int translationTable) {
        int[] starts = SequenceTools.findStartCodon(sequence, translationTable);
        int[] stops = SequenceTools.findTerminatorCodon(sequence, translationTable);
        if (starts == null || stops == null) {
            return null;
        }
        Orf longest = null;
        for (int start : starts) {
            int stop = findMatchingStop(start, stops);
            if (stop == -1) {
                continue;
            }
            Orf candidate = new Orf(start, stop + 2);
            if (longest == null || candidate.length() > longest.length()) {
                longest = candidate;
            }
        }
        return longest;
    }

    private int findMatchingStop(int start, int[] stops) {
        int frame = start % 3;
        for (int stop : stops) {
            if (stop > start && stop % 3 == frame) {
                return stop;
            }
        }
        return -1;
    }

    private record Orf(int start, int end) {
        int length() {
            return end - start;
        }
    }
}
