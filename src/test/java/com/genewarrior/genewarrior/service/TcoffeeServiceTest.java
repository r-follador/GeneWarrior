package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TcoffeeServiceTest {
    static List<SequenceDto> sequences() {
        return List.of(new SequenceDto("one", "Duplicate name", SequenceKind.DNA, "ACGT", List.of()),
                new SequenceDto("two", "Duplicate name", SequenceKind.DNA, "ACT", List.of()));
    }

    @Test
    void restoresInputOrderAndNamesWithNullableScoreAndProvenance() {
        var alignment = TcoffeeService.parse("id", sequences(), ">seq0002\nAC-T\n>seq0001\nACGT\n", "13.41");
        assertEquals("ACGT", alignment.sequences().getFirst().value());
        assertEquals("Duplicate name", alignment.sequences().get(1).name());
        assertEquals("** *", alignment.consensus());
        assertNull(alignment.score());
        assertEquals(64, alignment.provenance().sources().getFirst().sha256().length());
    }

    @Test
    void rejectsMissingDuplicateChangedAndUnequalRows() {
        for (String output : List.of(">seq0001\nACGT", ">seq0001\nACGT\n>seq0001\nAC-T",
                ">seq0001\nACGA\n>seq0002\nAC-T", ">seq0001\nACGT\n>seq0002\nACT",
                ">unknown\nACGT\n>seq0002\nAC-T"))
            assertThrows(ToolExecutionException.class, () -> TcoffeeService.parse("id", sequences(), output, "13.41"));
    }

    @Test
    void validatesBiologyAndLimitsBeforeSubmission() {
        var service = new TcoffeeService(mock(ExternalToolRunner.class), new GeneWarriorToolProperties(null, null, null, 0, 0, 0));
        for (String value : List.of("ACGN", "ACGU", "AC-T", "ACGT*")) {
            var source = new SequenceDto("two", "Input", SequenceKind.DNA, value, List.of());
            assertThrows(IllegalArgumentException.class, () -> service.validate(List.of(sequences().getFirst(), source)));
        }
        assertThrows(PayloadTooLargeException.class, () -> service.validate(List.of(sequences().getFirst(),
                new SequenceDto("two", "Long", SequenceKind.DNA, "A".repeat(2001), List.of()))));
    }
}
