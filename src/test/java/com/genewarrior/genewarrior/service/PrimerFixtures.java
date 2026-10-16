package com.genewarrior.genewarrior.service;

import com.genewarrior.genewarrior.api.dto.*;

import java.util.List;

public final class PrimerFixtures {
    public static SequenceDto source() {
        return new SequenceDto("template", "Test template", SequenceKind.DNA, "AACCGTTAGCGTACGATGCA".repeat(10), List.of());
    }

    public static PrimerSettings settings(boolean probe) {
        var constraints = new PrimerSettings.OligoConstraints(18, 20, 25, 55, 60, 70, 20, 80);
        return new PrimerSettings(100, 250, 5, constraints, probe, constraints, new PrimerSettings.Region(80, 100), 50, 1.5, 0.6, 50);
    }

    public static String output(boolean probe) {
        return "PRIMER_PAIR_NUM_RETURNED=1\nPRIMER_PAIR_0_PRODUCT_SIZE=169\nPRIMER_PAIR_0_PENALTY=1.2\n"
                + oligo("LEFT", 2, 20, false) + oligo("RIGHT", 170, 20, true)
                + (probe ? oligo("INTERNAL", 60, 20, false) : "") + "PRIMER_PAIR_EXPLAIN=considered 10, ok 1\n=\n";
    }

    private static String oligo(String type, int position, int length, boolean reverse) {
        int start = reverse ? position - length + 1 : position;
        String value = source().sequence().substring(start, start + length);
        if (reverse) {
            StringBuilder result = new StringBuilder();
            for (int i = value.length() - 1; i >= 0; i--) result.append("TGCA".charAt("ACGT".indexOf(value.charAt(i))));
            value = result.toString();
        }
        String prefix = "PRIMER_" + type + "_0";
        return prefix + "=" + position + "," + length + "\n" + prefix + "_SEQUENCE=" + value + "\n"
                + prefix + "_TM=60.1\n" + prefix + "_GC_PERCENT=50.0\n" + prefix + "_PENALTY=0.6\n";
    }

    public static PrimerDesignDto result(String id, boolean probe) {
        return PrimerDesignService.parse(id, source(), settings(probe), output(probe), "2.6.1");
    }
}
