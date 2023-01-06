package com.genewarrior.genetools.sequenceHandling;

/**
 * Holds a Sequence (SequenceAA) and a corresponding name (as String)
 * used for FASTA files
 */
public class NamedSequenceAA implements Cloneable {
    private static final long serialVersionUID = 7185806490860658722L;

    private SequenceAA seq;
    private String name;


    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }

    }

    public NamedSequenceAA(String name, SequenceAA seq) {
        this.seq = seq;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SequenceAA getSequence() {
        return seq;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSequence(SequenceAA seq) {
        this.seq = seq;
    }
}
