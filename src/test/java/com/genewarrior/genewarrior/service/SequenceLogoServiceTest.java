package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SequenceLogoServiceTest {
    private final SequenceLogoService service = new SequenceLogoService();

    private SequenceLogoDto logo(SequenceKind kind, String... sequences) {
        return service.calculate(new LogoRequest("source", "Logo", kind, LogoMode.INFORMATION,
                java.util.Arrays.stream(sequences).map(s -> new AlignmentSequenceDto("Sequence", s)).toList()));
    }

    @Test
    void calculatesUniformAndConservedDnaColumns() {
        var result = logo(SequenceKind.DNA, "AA", "AC", "AG", "AT");
        var conserved = result.columns().get(0);
        assertEquals(2, conserved.information(), 1e-12);
        assertEquals(4, conserved.observations());
        assertEquals(1, conserved.symbols().getFirst().frequency(), 1e-12);
        assertEquals(2, conserved.symbols().getFirst().bits(), 1e-12);
        var uniform = result.columns().get(1);
        assertEquals(0, uniform.information(), 1e-12);
        uniform.symbols().forEach(symbol -> assertEquals(.25, symbol.frequency(), 1e-12));
        SequenceLogoService.validate(result);
    }

    @Test
    void matchesAnalyticalThreeToOneDistribution() {
        // H(3/4,1/4) = 0.8112781244591328; uniform DNA background has 2 bits.
        var column = logo(SequenceKind.DNA, "A", "A", "A", "C").columns().getFirst();
        assertEquals(1.188721875540867, column.information(), 1e-12);
        assertEquals(.8915414066556503, column.symbols().get(0).bits(), 1e-12);
        assertEquals(.2971804688852168, column.symbols().get(1).bits(), 1e-12);
    }

    @Test
    void ignoresGapsAndAmbiguitiesWithoutInventingCoverage() {
        var columns = logo(SequenceKind.DNA, "aU-N", "Ar-N", "AN-N", "A--N").columns();
        var mixed = columns.get(1);
        assertEquals(1, mixed.observations());
        assertEquals(1, mixed.gaps());
        assertEquals(2, mixed.ignored());
        assertEquals(1, mixed.symbols().get(3).count());
        assertEquals(2, mixed.information(), 1e-12);
        assertEquals(0, columns.get(2).information());
        assertEquals(4, columns.get(2).gaps());
        assertEquals(4, columns.get(3).ignored());
        columns.get(3).symbols().forEach(symbol -> assertEquals(0, symbol.frequency()));
    }

    @Test
    void usesTwentyCanonicalAminoAcidsAndExcludesNoncanonicalSymbols() {
        var conserved = logo(SequenceKind.PROTEIN, "w", "W", "X", "*", "B", "U", "O").columns().getFirst();
        assertEquals(2, conserved.observations());
        assertEquals(5, conserved.ignored());
        assertEquals(Math.log(20) / Math.log(2), conserved.information(), 1e-12);
        var uniform = logo(SequenceKind.PROTEIN, "ACDEFGHIKLMNPQRSTVWY".split(""));
        assertEquals(0, uniform.columns().getFirst().information(), 1e-12);
        SequenceLogoService.validate(uniform);
    }

    @Test
    void rejectsRaggedInvalidAndOversizedAlignments() {
        assertThrows(IllegalArgumentException.class, () -> logo(SequenceKind.DNA, "AC", "A"));
        assertThrows(IllegalArgumentException.class, () -> logo(SequenceKind.DNA, "AC!"));
        assertThrows(IllegalArgumentException.class, () -> logo(SequenceKind.DNA, ""));
        assertThrows(IllegalArgumentException.class, () -> logo(SequenceKind.DNA));
        assertThrows(PayloadTooLargeException.class, () -> logo(SequenceKind.DNA, "A".repeat(2001)));
        var rows = java.util.Collections.nCopies(1001, new AlignmentSequenceDto("Sequence", "A".repeat(2000)));
        assertThrows(PayloadTooLargeException.class, () -> service.calculate(new LogoRequest("source", "Logo", SequenceKind.DNA, LogoMode.FREQUENCY, rows)));
    }

    @Test
    void rejectsForgedPersistedLogoValues() {
        var valid = logo(SequenceKind.DNA, "A");
        var c = valid.columns().getFirst();
        var invalid = new SequenceLogoDto(valid.id(), valid.name(), valid.sourceAlignmentId(), valid.kind(), valid.mode(),
                valid.method(), valid.sequenceCount(), List.of(new SequenceLogoDto.Column(1, 1, 0, 0, Double.NaN, c.symbols())));
        assertThrows(IllegalArgumentException.class, () -> SequenceLogoService.validate(invalid));
    }
}
