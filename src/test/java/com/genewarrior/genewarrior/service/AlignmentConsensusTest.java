package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.AlignmentSequenceDto;
import com.genewarrior.genewarrior.api.dto.ConsensusRequest;
import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlignmentConsensusTest {
    private final AlignmentService service = new AlignmentService();

    private SequenceDto consensus(SequenceKind kind, String... rows) {
        return service.consensus(new ConsensusRequest("Alignment", kind,
                Arrays.stream(rows).map(value -> new AlignmentSequenceDto("Row", value)).toList()));
    }

    @Test
    void returnsIndependentSequenceWithNormalizedCaseAndUracil() {
        var result = consensus(SequenceKind.DNA, "acgu", "ACGT", "ACTT");
        assertEquals("ACGT", result.sequence());
        assertEquals("Alignment [Consensus]", result.name());
        assertEquals(SequenceKind.DNA, result.kind());
        assertEquals(List.of(), result.marks());
        assertNotEquals(result.id(), consensus(SequenceKind.DNA, "ACGT").id());
    }

    @Test
    void preservesLegacyDnaTieAndAmbiguityRules() {
        assertEquals("MRWSYK", consensus(SequenceKind.DNA, "AAACCG", "CGTGTT").sequence());
        assertEquals("N", consensus(SequenceKind.DNA, "A", "C", "G").sequence());
        assertEquals("N", consensus(SequenceKind.DNA, "A", "C", "G", "T").sequence());
        assertEquals("R", consensus(SequenceKind.DNA, "R", "R", "C").sequence());
        assertEquals("V", consensus(SequenceKind.DNA, "R", "C").sequence());
        assertEquals("N", consensus(SequenceKind.DNA, "N", "A").sequence());
    }

    @Test
    void removesGapDominatedColumnsButKeepsGapTies() {
        assertEquals("AT", consensus(SequenceKind.DNA, "A-C--T", "A----T", "A----T").sequence());
        assertEquals("ACGT", consensus(SequenceKind.DNA, "ACGT", "----").sequence());
        assertEquals("M", consensus(SequenceKind.DNA, "A", "C", "-").sequence());
        assertEquals("T", consensus(SequenceKind.DNA, "AT", "CT", "-T", "-T").sequence());
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.DNA, "--", "--"));
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.DNA, "A", "-", "-"));
    }

    @Test
    void supportsProteinTiesStopsAndExtendedAlphabet() {
        var result = consensus(SequenceKind.PROTEIN, "WMXBZJUO*-", "WAXBZJUO*-");
        assertEquals("WXXBZJUO*", result.sequence());
        assertEquals(SequenceKind.PROTEIN, result.kind());
        assertEquals("X", consensus(SequenceKind.PROTEIN, "*", "A").sequence());
        assertEquals("M", consensus(SequenceKind.PROTEIN, "M", "M", "W").sequence());
    }

    @Test
    void rejectsMissingRaggedAndInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.DNA));
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.DNA, ""));
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.DNA, "AC", "A"));
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.DNA, "A!"));
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.DNA, "AX"));
        assertThrows(IllegalArgumentException.class, () -> consensus(SequenceKind.PROTEIN, "M1"));
        assertThrows(IllegalArgumentException.class, () -> service.consensus(null));
        assertThrows(IllegalArgumentException.class, () -> service.consensus(
                new ConsensusRequest("Alignment", SequenceKind.DNA, Collections.singletonList((AlignmentSequenceDto) null))));
    }

    @Test
    void boundsWorkAndResultName() {
        assertThrows(PayloadTooLargeException.class, () -> consensus(SequenceKind.DNA, "A".repeat(100_001)));
        var row = new AlignmentSequenceDto("Row", "A".repeat(1000));
        assertThrows(PayloadTooLargeException.class, () -> service.consensus(
                new ConsensusRequest("Alignment", SequenceKind.DNA, Collections.nCopies(2001, row))));
        assertThrows(PayloadTooLargeException.class, () -> service.consensus(
                new ConsensusRequest("Alignment", SequenceKind.DNA,
                        Collections.nCopies(10_001, new AlignmentSequenceDto("Row", "A")))));
        var result = service.consensus(new ConsensusRequest("a".repeat(255), SequenceKind.DNA, List.of(row)));
        assertEquals(255, result.name().length());
        assertTrue(result.name().endsWith(" [Consensus]"));
    }
}
