package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.SequenceDto;
import com.genewarrior.genewarrior.api.dto.SequenceKind;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SequenceImportService {
    public List<SequenceDto> importText(String text) {
        List<ParsedSequence> parsed = text.stripLeading().startsWith(">") ? parseFasta(text) : List.of(new ParsedSequence("Sequence 1", text));
        return parsed.stream()
                .map(sequence -> {
                    String normalized = normalize(sequence.sequence());
                    SequenceKind kind = detectKind(normalized);
                    return new SequenceDto(UUID.randomUUID().toString(), sequence.name(), kind, normalized, List.of());
                })
                .toList();
    }

    private List<ParsedSequence> parseFasta(String text) {
        List<ParsedSequence> sequences = new ArrayList<>();
        String currentName = null;
        StringBuilder currentSequence = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (line.startsWith(">")) {
                addCurrent(sequences, currentName, currentSequence);
                currentName = line.substring(1).trim();
                if (currentName.isEmpty()) {
                    currentName = "Untitled sequence";
                }
                currentSequence = new StringBuilder();
            } else {
                currentSequence.append(line);
            }
        }
        addCurrent(sequences, currentName, currentSequence);
        if (sequences.isEmpty()) {
            throw new IllegalArgumentException("No sequences found");
        }
        return sequences;
    }

    private void addCurrent(List<ParsedSequence> sequences, String name, StringBuilder sequence) {
        if (name != null) {
            String normalized = normalize(sequence.toString());
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Sequence '%s' is empty".formatted(name));
            }
            sequences.add(new ParsedSequence(name, normalized));
        }
    }

    private String normalize(String sequence) {
        return sequence.replaceAll("[\\s\\-.0-9]", "").toUpperCase().replace('U', 'T');
    }

    private SequenceKind detectKind(String sequence) {
        if (sequence.matches("[ACGTRYSWKMBDHVN]+")) {
            return SequenceKind.DNA;
        }
        if (sequence.matches("[ABCDEFGHIKLMNPQRSTVWYZ\\*X]+")) {
            return SequenceKind.PROTEIN;
        }
        throw new IllegalArgumentException("Sequence contains unsupported characters");
    }

    private record ParsedSequence(String name, String sequence) {
    }
}
