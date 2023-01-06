package com.genewarrior.genetools.alignment;

import com.genewarrior.genetools.Feature;
import com.genewarrior.genetools.sequenceHandling.NamedSequenceAA;
import com.genewarrior.genetools.sequenceHandling.NamedSequenceDNA;

public class Rank implements Comparable<Rank> {
    public int rank;
    public Feature feature;
    public NamedSequenceDNA seqdna;
    public NamedSequenceAA seqaa;
    public String name;

    public Rank(int rank, String name) {
        this.rank = rank;
        this.name = name;
    }

    public Rank(int rank, Feature seq) {
        this.rank = rank;
        this.feature = seq;
    }

    public Rank(int rank, NamedSequenceDNA seq) {
        this.rank = rank;
        this.seqdna = seq;
    }

    public Rank(int rank, NamedSequenceAA seq) {
        this.rank = rank;
        this.seqaa = seq;
    }

    @Override
    public int compareTo(Rank r) {
        return rank - r.rank;
    }
}
