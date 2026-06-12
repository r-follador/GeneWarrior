package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslationServiceTest {
    private final TranslationService service = new TranslationService();

    @Test
    void translatesReverseFrameWithReadableFrameName() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "ATGAAA", List.of());

        var translated = service.translate(sequence, 4, 1);

        assertThat(translated.sequence()).isEqualTo("FH");
        assertThat(translated.name()).endsWith("[Translated Reverse Frame 1]");
    }

    @Test
    void usesSelectedTranslationTable() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "ATA", List.of());

        assertThat(service.translate(sequence, 1, 1).sequence()).isEqualTo("I");
        assertThat(service.translate(sequence, 1, 2).sequence()).isEqualTo("M");
    }

    @Test
    void rejectsUnsupportedTranslationTable() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "ATG", List.of());

        assertThatThrownBy(() -> service.translate(sequence, 1, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported translation table 7");
    }

    @Test
    void translatesBestForwardOrf() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "CCCATGAAATAACCC", List.of());

        var translated = service.translateBestOrf(sequence, 1);

        assertThat(translated.sequence()).isEqualTo("MK*");
        assertThat(translated.name()).endsWith("[Longest ORF: 4-12nt]");
    }

    @Test
    void translatesBestReverseOrf() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "TTATTTCATGGG", List.of());

        var translated = service.translateBestOrf(sequence, 1);

        assertThat(translated.sequence()).isEqualTo("MK*");
        assertThat(translated.name()).endsWith("[Longest ORF: RevComp 4-12nt]");
    }

    @Test
    void reportsWhenNoOrfExists() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "CCCCCC", List.of());

        assertThatThrownBy(() -> service.translateBestOrf(sequence, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No ORF found");
    }

    @Test
    void returnsForwardFramePreviewWithStartAndStopMarkers() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "CCCATGAAATAACCC", List.of());

        var frames = service.frames(sequence, 1);

        assertThat(frames.frame1()).isEqualTo("PMK*P");
        assertThat(frames.markers1()).isEqualTo("oaobo");
    }

    @Test
    void translatesSelectedCodonRange() {
        var sequence = new SequenceDto("dna", "alpha", SequenceKind.DNA, "CCCATGAAATAACCC", List.of());

        var translated = service.translateRange(sequence, 3, 11, 1);

        assertThat(translated.sequence()).isEqualTo("MK*");
        assertThat(translated.name()).endsWith("[Translated 4-12]");
    }
}
