/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.sequenceHandling;


/**
 * @author kingcarlxx
 */
public class SequenceDNA extends Sequence {

    /**
     *
     */
    private static final long serialVersionUID = -913788407021051520L;
    private final static Nucleotide nucl = new Nucleotide();
    private boolean isRNA = false;
    private transient int[] NucleotideCount = new int[17];

    /**
     * Create <code>SequenceDNA</code> using a string, illegal nucleotides are converted to Gaps (-). Set <code>isRNA</code> to true, if RNA, false if DNA.
     *
     * @param seq
     * @param isRNA
     */
    public SequenceDNA(String seq, boolean isRNA) {
        setSequence(seq);
        this.isRNA = isRNA;
    }

    /**
     * Create <code>SequenceDNA</code> using a byte array. Set <code>isRNA</code> to true, if RNA, false if DNA.
     *
     * @param seq
     * @param isRNA
     */
    public SequenceDNA(byte[] seq, boolean isRNA) {
        sequence = seq;
        this.isRNA = isRNA;
    }

    public SequenceDNA() {
        sequence = new byte[0];
    }


    /**
     * Returns Subsequence of given Position, assumes that this <code>SequenceDNA</code> is linear.
     *
     * @param pos
     * @return null if illegal position
     */
    public SequenceDNA getLinearSubSequence(Position pos) {
        int startPos = pos.getStartPos();
        int endPos = pos.getEndPos();

        if (startPos > getLength() - 1 || endPos > getLength() - 1 || startPos > endPos)
            return null;

        byte[] subArray = new byte[endPos - startPos + 1];
        System.arraycopy(sequence, startPos, subArray, 0, endPos - startPos + 1);

        SequenceDNA subSequence = new SequenceDNA(subArray, isRNA);
        if (pos.isReverse())
            subSequence = SequenceTools.reverseComplement(subSequence, true, true);

        return subSequence;
    }

    /**
     * Returns Subsequence of given Position, assumes that this <code>SequenceDNA</code> is circular. So Position can be wrap-over.
     * Includes all positions
     *
     * @param pos
     * @return
     */
    public SequenceDNA getCircularSubSequence(Position pos) {
        int startPos = pos.getStartPos();
        int endPos = pos.getEndPos();
        byte[] subArray;

        if (startPos > getLength() - 1 || endPos > getLength() - 1)
            return null;

        if (startPos <= endPos) //normal case
        {
            subArray = new byte[endPos - startPos + 1];
            System.arraycopy(sequence, startPos, subArray, 0, endPos - startPos + 1);
        } else    //in case of Position over zero
        {
            subArray = new byte[getLength() - startPos + endPos + 1];
            System.arraycopy(sequence, startPos, subArray, 0, getLength() - startPos);
            System.arraycopy(sequence, 0, subArray, getLength() - startPos, endPos + 1);
        }

        SequenceDNA subSequence = new SequenceDNA(subArray, isRNA);
        if (pos.isReverse())
            subSequence = SequenceTools.reverseComplement(subSequence, true, true);

        return subSequence;


    }

    /**
     * @return true if Sequence is set to RNA, false if DNA
     */
    public boolean isRNA() {
        return this.isRNA;
    }

    /**
     * Sets sequence of this object using a string
     *
     * @param seq
     */
    public void setSequence(String seq) {
        sequence = nucl.string2bytearray(seq);
    }

    /**
     * Returns this sequence as string
     *
     * @return
     */
    public String toString() {
        return nucl.bytearray2string(sequence);
    }

    /**
     * Converts this Sequence to RNA (replaces all Ts through Us)
     */
    public void convert2RNA() {
        isRNA = true;
        sequence = nucl.changeByte(sequence, Nucleotide._T, Nucleotide._U);
    }

    /**
     * Converts this Sequence to DNA (replaces all Us through Ts)
     */
    public void convert2DNA() {
        isRNA = false;
        sequence = nucl.changeByte(sequence, Nucleotide._U, Nucleotide._T);
    }

    /**
     * Deletes all Gaps (-)
     */
    public void killGaps() {
        sequence = nucl.killGaps(sequence);
        updateNucleotideCount();
    }

    public void setByteArray(byte[] newbytearray) {
        super.setByteArray(newbytearray);
    }

    /**
     * Updates NucleotideCount Array
     */
    public void updateNucleotideCount() {
        NucleotideCount = nucl.countNucleots(sequence);
    }

    /**
     * Get NucleotideCount Array
     *
     * @return Int-Array: Index of Array: Nucleotide-Value in <code>Nucleotide</code> class; Value of Array: counted occurrences of this nucleotide
     */
    public int[] getNucleotideCount() {
        return NucleotideCount;
    }
}
