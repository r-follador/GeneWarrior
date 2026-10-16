package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;
import com.genewarrior.genewarrior.config.GeneWarriorToolProperties;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GuideTreeServiceTest {
    private final TcoffeeService tcoffee = new TcoffeeService(mock(ExternalToolRunner.class), new GeneWarriorToolProperties(null, null, null, 0, 0, 0));
    private final GuideTreeService service = new GuideTreeService(tcoffee, mock(ExternalToolRunner.class), null);

    TreeRequest request() {
        return new TreeRequest(UUID.randomUUID(), "alignment", "Example", SequenceKind.DNA, List.of(
                new AlignmentSequenceDto("O'Brien <sample>", "ACGT"), new AlignmentSequenceDto("Duplicate", "AC-T"), new AlignmentSequenceDto("Duplicate", "A-GT")));
    }

    @Test
    void parsesMultifurcationsAndPreservesNamesIndicesSignedAndScientificLengths() {
        var request = request();
        var result = GuideTreeService.parse(request, service.validate(request), "(seq0003:0,seq0001:-0.02,seq0002:1.5e-2);", "13.41");
        assertEquals(List.of(2, 0, 1), result.nodes().subList(1, 4).stream().map(GuideTreeDto.Node::sourceIndex).toList());
        assertEquals(-0.02, result.nodes().get(2).length());
        assertEquals(0.015, result.nodes().get(3).length());
        assertEquals("O'Brien <sample>", result.nodes().get(2).name());
        assertEquals(64, result.sources().getFirst().sha256().length());
    }

    @Test
    void acceptsZeroLengthAndNestedTrees() {
        var request = request();
        var result = GuideTreeService.parse(request, service.validate(request), "((seq0001:0,seq0002:0):0,seq0003:0);", "13.41");
        assertEquals(5, result.nodes().size());
        GuideTreeService.validateResult(result);
    }

    @Test
    void rejectsMalformedTreesAndMissingDuplicateOrUnexpectedLeaves() {
        var request = request();
        for (String tree : List.of("(seq0001:0,seq0002:0);", "(seq0001:0,seq0002:0,seq0002:0);", "(bad:0,seq0002:0,seq0003:0);",
                "(seq0001:NaN,seq0002:0,seq0003:0);", "(seq0001:1e999,seq0002:0,seq0003:0);", "(seq0001,seq0002,seq0003);",
                "(seq0001:0,seq0002:0,seq0003:0)", "(seq0001:0,seq0002:0,seq0003:0);(a,b);", "(".repeat(41) + "seq0001:0" + ")".repeat(41) + ";"))
            assertThrows(ToolExecutionException.class, () -> GuideTreeService.parse(request, service.validate(request), tree, "13.41"), tree);
    }

    @Test
    void validatesAlignmentAndUngappedLimits() {
        var request = request();
        assertEquals("ACT", service.validate(request).get(1).sequence());
        for (String sequence : List.of("ACN-", "----", "ACG", "A".repeat(10001))) {
            var bad = new TreeRequest(request.requestId(), request.sourceAlignmentId(), request.name(), request.kind(),
                    List.of(request.sequences().getFirst(), request.sequences().get(1), new AlignmentSequenceDto("Bad", sequence)));
            assertThrows(RuntimeException.class, () -> service.validate(bad));
        }
    }

    @Test
    void rejectsCyclesAndInventedLeafMappingsInSharedTrees() {
        var request = request();
        var result = GuideTreeService.parse(request, service.validate(request), "(seq0001:0,seq0002:0,seq0003:0);", "13.41");
        var nodes = new ArrayList<>(result.nodes());
        nodes.set(1, new GuideTreeDto.Node(1, 1, nodes.get(1).name(), 0, 0d));
        assertThrows(IllegalArgumentException.class, () -> GuideTreeService.validateResult(new GuideTreeDto(result.id(), result.name(), result.sourceAlignmentId(), result.kind(), result.version(), result.method(), nodes, result.sources())));
    }
}
