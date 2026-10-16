package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrimerDesignServiceTest {
    @TempDir
    Path parameters;

    @Test
    void convertsRightCoordinatesAndPreservesProbeStrandAndSource() {
        var result = PrimerFixtures.result("test", true);
        var pair = result.pairs().getFirst();
        assertEquals(2, pair.left().start());
        assertEquals(151, pair.right().start());
        assertEquals(171, pair.right().end());
        assertEquals("REVERSE", pair.right().strand());
        assertNotEquals(result.source().sequence().substring(151, 171), pair.right().sequence());
        assertEquals("FORWARD", pair.probe().strand());
        assertEquals(169, pair.productSize());
        assertEquals(PrimerFixtures.settings(true), result.settings());
    }

    @Test
    void zeroPairsIsSuccessfulWithExplanations() {
        var result = PrimerDesignService.parse("none", PrimerFixtures.source(), PrimerFixtures.settings(true),
                "PRIMER_PAIR_NUM_RETURNED=0\nPRIMER_LEFT_EXPLAIN=considered 20, low tm 20, ok 0\n=\n", "2.6.1");
        assertTrue(result.pairs().isEmpty());
        assertTrue(result.explanations().getFirst().contains("low tm"));
    }

    @Test
    void rejectsBrokenOutputWrongCoordinatesAndMissingProbe() {
        String valid = PrimerFixtures.output(true);
        for (String broken : List.of(valid.replace("PRIMER_RIGHT_0=170,20", "PRIMER_RIGHT_0=169,20"),
                valid.replace("PRODUCT_SIZE=169", "PRODUCT_SIZE=170"), valid.replace("_TM=60.1", "_TM=NaN"),
                valid.replace("=\n", ""), PrimerFixtures.output(false), "PRIMER_PAIR_NUM_RETURNED=1\nPRIMER_PAIR_NUM_RETURNED=1\n=\n")) {
            assertThrows(ToolExecutionException.class, () -> PrimerDesignService.parse("bad", PrimerFixtures.source(), PrimerFixtures.settings(true), broken, "2.6.1"));
        }
        assertEquals("TOOL_FAILED", assertThrows(ToolExecutionException.class, () -> PrimerDesignService.parse("bad", PrimerFixtures.source(),
                PrimerFixtures.settings(false), "PRIMER_ERROR=private path or raw diagnostic\n=\n", "2.6.1")).code());
    }

    @Test
    void validatesInputAlphabetLimitsAndOrderedConstraints() {
        for (String input : List.of("ACGT", "ACGU".repeat(20), "ACG-".repeat(20), "ACGR".repeat(20), "A\nPRIMER_TASK=generic")) {
            assertThrows(IllegalArgumentException.class, () -> PrimerDesignService.validate(new SequenceDto("id", "Name", SequenceKind.DNA, input, List.of()), PrimerFixtures.settings(false)));
        }
        assertThrows(PayloadTooLargeException.class, () -> PrimerDesignService.validate(new SequenceDto("id", "Name", SequenceKind.DNA, "A".repeat(20001), List.of()), PrimerFixtures.settings(false)));
        var s = PrimerFixtures.settings(false);
        var invalid = new PrimerSettings(250, 100, 5, s.primers(), false, s.probe(), null, 50, 1.5, 0.6, 50);
        assertThrows(IllegalArgumentException.class, () -> PrimerDesignService.validate(PrimerFixtures.source(), invalid));
        assertEquals("AACCG", PrimerDesignService.validate(PrimerFixtures.source(), s).sequence().substring(0, 5));
    }

    @Test
    void serializesAllowlistedBoulderTagsAndProbesThermodynamicData() throws Exception {
        Files.writeString(parameters.resolve("stack.dh"), "fixture");
        var runner = mock(ExternalToolRunner.class);
        when(runner.available("/test/primer3")).thenReturn(true);
        when(runner.run(List.of("/test/primer3", "--about"), "")).thenReturn(new ExternalToolRunner.ToolRunResult(0, "libprimer3 release 2.6.1\n", "", null));
        when(runner.run(eq(List.of("/test/primer3", "--strict_tags")), anyString())).thenReturn(new ExternalToolRunner.ToolRunResult(0, "PRIMER_PAIR_NUM_RETURNED=0\n=\n", "", null));
        var service = new PrimerDesignService(runner, new GeneWarriorToolProperties(null, new GeneWarriorToolProperties.Primer3("/test/primer3", parameters.toString()), null, 0, 0, 0));
        String boulder = service.boulder(PrimerFixtures.source(), PrimerFixtures.settings(true));
        assertTrue(boulder.contains("SEQUENCE_TARGET=80,20\n"));
        assertTrue(boulder.contains("PRIMER_PICK_INTERNAL_OLIGO=1\n"));
        assertTrue(boulder.contains("PRIMER_THERMODYNAMIC_TEMPLATE_ALIGNMENT=1\n"));
        assertFalse(boulder.contains(PrimerFixtures.source().name()));
        service.probe();
        assertTrue(service.capabilities().available());
        Files.delete(parameters.resolve("stack.dh"));
        assertFalse(service.capabilities().available());
    }
}
