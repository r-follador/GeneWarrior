package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import com.genewarrior.genewarrior.api.dto.SequenceMarkDto;
import com.genewarrior.genewarrior.api.dto.TransformType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SequenceTransformServiceTest {
    private final SequenceTransformService service = new SequenceTransformService();

    @Test
    void reverseComplementsDnaAndMarks() {
        var sequence = new SequenceDto("s1", "alpha", SequenceKind.DNA, "ACGTRY", List.of(new SequenceMarkDto(1, 3, "#ff0000", "x")));

        var transformed = service.transform(sequence, TransformType.REVERSE_COMPLEMENT);

        assertThat(transformed.sequence()).isEqualTo("RYACGT");
        assertThat(transformed.marks().getFirst().start()).isEqualTo(3);
        assertThat(transformed.marks().getFirst().end()).isEqualTo(5);
    }
}
