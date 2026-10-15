package com.genewarrior.genewarrior.api.dto;

import java.util.List;

public record SequenceLogoDto(
        String id, String name, String sourceAlignmentId, SequenceKind kind,
        LogoMode mode, String method, int sequenceCount, List<Column> columns
) {
    public record Symbol(String residue, int count, double frequency, double bits) {
    }

    public record Column(int position, int observations, int gaps, int ignored,
                         double information, List<Symbol> symbols) {
    }
}
