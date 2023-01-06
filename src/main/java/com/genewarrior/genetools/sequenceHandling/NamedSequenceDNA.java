package com.genewarrior.genetools.sequenceHandling;

/**
 * Holds a Sequence (SequenceDNA) and a corresponding name (as String)
 * used for FASTA files
 */
public class NamedSequenceDNA implements Cloneable {

    private static final long serialVersionUID = 7689806490860658722L;

    private SequenceDNA seq;
    private String name;


    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }

    }

    public NamedSequenceDNA(String name, SequenceDNA seq) {
        this.seq = seq;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SequenceDNA getSequence() {
        return seq;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSequence(SequenceDNA seq) {
        this.seq = seq;
    }
}
