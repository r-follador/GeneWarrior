package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ProjectValidation {
    private ProjectValidation() {
    }

    static void validate(ProjectDto project) {
        require(project != null, "Project is required");
        name(project.name());
        require(project.sequences().size() <= 10000 && project.alignments().size() <= 10000
                && project.logos().size() <= 10000 && project.primerDesigns().size() <= 10000 && project.entryOrder().size() <= 10000, "Too many project entries");
        Set<String> ids = new HashSet<>();
        for (SequenceDto sequence : project.sequences()) {
            require(sequence != null, "Sequence is required");
            entryId(sequence.id(), ids);
            name(sequence.name());
            sequence(sequence.sequence(), sequence.kind());
            marks(sequence.marks(), sequence.sequence().length(), false);
        }
        for (AlignmentDto alignment : project.alignments()) {
            require(alignment != null, "Alignment is required");
            entryId(alignment.id(), ids);
            name(alignment.name());
            require(alignment.sequences().size() >= 2 && alignment.sequences().size() <= 10000,
                    "Alignment requires at least two rows");
            int length = -1;
            for (AlignmentSequenceDto row : alignment.sequences()) {
                require(row != null, "Alignment row is required");
                name(row.name());
                sequence(row.value(), alignment.kind());
                if (length == -1) length = row.value().length();
                require(row.value().length() == length, "Alignment rows must have equal lengths");
            }
            require(alignment.consensus() != null && alignment.consensus().length() == length
                    && alignment.consensus().matches("[*:. ]*"), "Invalid alignment consensus");
            marks(alignment.marks(), length, true);
        }
        for (SequenceLogoDto logo : project.logos()) {
            SequenceLogoService.validate(logo);
            entryId(logo.id(), ids);
            name(logo.name());
        }
        for (PrimerDesignDto design : project.primerDesigns()) {
            PrimerDesignService.validateResult(design);
            entryId(design.id(), ids);
            name(design.name());
        }
        Set<String> order = new HashSet<>(project.entryOrder());
        require(order.size() == project.entryOrder().size() && order.equals(ids), "Invalid workspace entry order");
    }

    private static void entryId(String id, Set<String> ids) {
        require(id != null && id.length() <= 160 && id.matches("[a-zA-Z0-9_-]+"), "Invalid entry ID");
        require(ids.add(id), "Duplicate entry IDs");
    }

    private static void name(String value) {
        require(value != null && !value.isBlank() && value.length() <= 255, "Invalid project or entry name");
    }

    private static void sequence(String value, SequenceKind kind) {
        require(kind != null && value != null && !value.isEmpty(), "Sequence and kind are required");
        require(value.matches(kind == SequenceKind.DNA ? "(?i)[ACGTURYSWKMBDHVN-]+" : "(?i)[A-Z*\\-]+"),
                "Invalid sequence characters");
    }

    private static void marks(List<SequenceMarkDto> marks, int length, boolean alignment) {
        require(marks.size() <= 10000, "Too many annotations");
        for (SequenceMarkDto mark : marks) {
            require(mark != null && mark.start() >= 0 && mark.end() > mark.start() && mark.end() <= length,
                    "Invalid annotation coordinates");
            require(mark.color() != null && mark.color().matches("#[a-fA-F0-9]{6}"), "Invalid annotation color");
            boolean arrow = "ANNOTATION".equals(mark.type());
            require(arrow || (!alignment && (mark.type() == null || "HIGHLIGHT".equals(mark.type()))),
                    "Invalid annotation type");
            require(mark.direction() == null || "FORWARD".equals(mark.direction()) || "REVERSE".equals(mark.direction()),
                    "Invalid annotation direction");
            require(mark.annotation() == null || (!mark.annotation().isBlank() && mark.annotation().length() <= 100),
                    "Invalid annotation label");
            require(!arrow || mark.annotation() != null, "Annotation label is required");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
