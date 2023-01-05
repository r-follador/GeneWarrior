/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.sequenceHandling;

import java.io.Serializable;


/**
 * A <code>Sequence</code> object represents a sequence of amino acid,
 * or nucleotide acid (RNA/DNA)
 *
 * @author kingcarlxx
 */
public class Sequence implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -917788507021051520L;
    protected byte[] sequence;

    public Sequence() {

    }


    /**
     * Get Length of Sequence
     *
     * @return
     */
    public int getLength() {
        return sequence.length;
    }

    /**
     * Return Byte-Array of Sequence
     *
     * @return
     */
    public byte[] getByteArray() {
        return sequence;
    }

    /**
     * Sets a new sequence directly via bytearray
     *
     * @param newbytearray
     */
    public void setByteArray(byte[] newbytearray) {
        sequence = newbytearray;
    }

    /**
     * Compares Sequence to another. Checks every position.
     *
     * @param otherObject
     * @return
     */
    public boolean equals(Object otherObject) {
        if (this == otherObject) return true;
        if (otherObject == null) return false;
        if (getClass() != otherObject.getClass()) return false;
        Sequence otherSeq = (Sequence) otherObject;

        byte[] seq1 = this.sequence;
        byte[] seq2 = otherSeq.getByteArray();

        if (seq1.length != seq2.length)
            return false;

        for (int i = 0; i < seq1.length; i++) {
            if (seq1[i] != seq2[i])
                return false;
        }
        return true;
    }

}
