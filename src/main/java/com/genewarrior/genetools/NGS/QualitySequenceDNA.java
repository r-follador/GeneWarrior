/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.NGS;


import com.genewarrior.genetools.sequenceHandling.SequenceDNA;

/**
 * Holds a Sequence (SequenceDNA) and a corresponding name (as String)
 * used for FASTA files
 */
public class QualitySequenceDNA implements Cloneable {
    /**
     *
     */
    private static final long serialVersionUID = 7689806690865658322L;

    private byte[] qual;
    private SequenceDNA seq;
    private String name;


    @Override
    public Object clone() {
        try {
            QualitySequenceDNA cloned = (QualitySequenceDNA) super.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            return null;
        }

    }

    public QualitySequenceDNA(String name, SequenceDNA seq, byte[] quality) {
        this.seq = seq;
        this.name = name;
        this.qual = quality;
        //TODO: throw error if length doesn't match to Sequence
    }

    public String getName() {
        return name;
    }

    public SequenceDNA getSequence() {
        return seq;
    }


    /**
     * returns the quality values (0-64) as a byte array corresponding to the position in the getSequence() SequenceDNA object
     *
     * @return
     */
    public byte[] getQuality() {
        return qual;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSequence(SequenceDNA seq) {
        this.seq = seq;
    }

    public void setQuality(byte[] quality) {
        //TODO: throw error if length doesn't match to Sequence
        this.qual = quality;
    }
}
