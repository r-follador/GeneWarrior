package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import com.genewarrior.genewarrior.api.dto.SequenceMarkDto;
import com.genewarrior.genewarrior.api.dto.TransformType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SequenceTransformService {
    public SequenceDto transform(SequenceDto sequence, TransformType type) {
        if (sequence.kind() != SequenceKind.DNA) {
            throw new IllegalArgumentException("Only DNA sequences can be transformed");
        }
        boolean reverse = type == TransformType.REVERSE || type == TransformType.REVERSE_COMPLEMENT;
        boolean complement = type == TransformType.COMPLEMENT || type == TransformType.REVERSE_COMPLEMENT;
        String value = sequence.sequence();
        if (complement) {
            value = complement(value);
        }
        if (reverse) {
            value = new StringBuilder(value).reverse().toString();
        }
        List<SequenceMarkDto> marks = reverse ? reverseMarks(sequence.marks(), sequence.sequence().length()) : sequence.marks();
        return new SequenceDto(UUID.randomUUID().toString(), sequence.name() + " [" + type.name().toLowerCase().replace('_', ' ') + "]", sequence.kind(), value, marks);
    }

    private String complement(String sequence) {
        StringBuilder out = new StringBuilder(sequence.length());
        for (char base : sequence.toCharArray()) {
            out.append(switch (base) {
                case 'A' -> 'T';
                case 'T', 'U' -> 'A';
                case 'G' -> 'C';
                case 'C' -> 'G';
                case 'R' -> 'Y';
                case 'Y' -> 'R';
                case 'S' -> 'S';
                case 'W' -> 'W';
                case 'K' -> 'M';
                case 'M' -> 'K';
                case 'B' -> 'V';
                case 'D' -> 'H';
                case 'H' -> 'D';
                case 'V' -> 'B';
                default -> 'N';
            });
        }
        return out.toString();
    }

    private List<SequenceMarkDto> reverseMarks(List<SequenceMarkDto> marks, int length) {
        return marks.stream()
                .map(mark -> new SequenceMarkDto(length - mark.end(), length - mark.start(), mark.color(), mark.annotation(), mark.type(), reverseDirection(mark.direction())))
                .toList();
    }

    private String reverseDirection(String direction) {
        if ("FORWARD".equals(direction)) {
            return "REVERSE";
        }
        if ("REVERSE".equals(direction)) {
            return "FORWARD";
        }
        return direction;
    }
}
