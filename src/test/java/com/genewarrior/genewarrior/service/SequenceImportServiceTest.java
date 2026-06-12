package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.SequenceKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SequenceImportServiceTest {
    private final SequenceImportService service = new SequenceImportService();

    @Test
    void importsFastaAndDetectsDna() {
        var sequences = service.importText(">alpha\nacg u\n>beta\nNNRY");

        assertThat(sequences).hasSize(2);
        assertThat(sequences.getFirst().name()).isEqualTo("alpha");
        assertThat(sequences.getFirst().kind()).isEqualTo(SequenceKind.DNA);
        assertThat(sequences.getFirst().sequence()).isEqualTo("ACGT");
    }

    @Test
    void detectsProteinSequence() {
        var sequences = service.importText(">protein\nMKWVTFISLL*");

        assertThat(sequences).hasSize(1);
        assertThat(sequences.getFirst().kind()).isEqualTo(SequenceKind.PROTEIN);
    }

    @Test
    void rejectsUnsupportedCharacters() {
        assertThatThrownBy(() -> service.importText(">bad\nACGT!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
    }
}
