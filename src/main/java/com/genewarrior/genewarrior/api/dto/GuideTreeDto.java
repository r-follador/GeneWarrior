package com.genewarrior.genewarrior.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

import com.genewarrior.genewarrior.api.dto.AlignmentProvenanceDto.Source;

public record GuideTreeDto(
        @NotBlank String id, @NotBlank @Size(max = 255) String name,
        @NotBlank String sourceAlignmentId, @NotNull SequenceKind kind,
        @NotBlank String version, @NotBlank String method,
        @NotNull @Size(min = 3, max = 40) List<@NotNull @Valid Node> nodes,
        @NotNull @Size(min = 2, max = 20) List<@NotNull @Valid Source> sources
) {
    // Preorder IDs keep the persisted representation flat and bounded, including multifurcations.
    public record Node(int id, Integer parentId, String name, Integer sourceIndex, Double length) {
    }
}
