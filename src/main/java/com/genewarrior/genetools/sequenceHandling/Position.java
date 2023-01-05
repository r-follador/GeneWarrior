/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.sequenceHandling;

import java.io.Serializable;

/**
 * Position on Molecule
 *
 * @author kingcarlxx
 */
public class Position implements Serializable, Cloneable {
    /**
     *
     */
    private static final long serialVersionUID = -4723305680320652570L;
    private int startingPos = 0;
    private int endingPos = 0;
    private boolean singlePos = true;
    private boolean complement = false;

    /**
     * Creates new Position with a singlePosition
     *
     * @param singlePos
     */
    public Position(int singlePos) {
        startingPos = singlePos;
        endingPos = singlePos;
        this.singlePos = true;
    }

    /**
     * Creates new Position starting from startingPos to endingPos
     *
     * @param startingPos
     * @param endingPos
     * @param complement  is position on other strand, going in other direction?
     */
    public Position(int startingPos, int endingPos, boolean complement) {
        this.startingPos = startingPos;
        this.endingPos = endingPos;

        if (startingPos != endingPos) {
            this.singlePos = false;

            this.complement = complement;
        }


    }

    /**
     * Checks if given Integer of this <code>Position</code>
     *
     * @param integer
     * @param pos
     * @param isCircular indicates if the positions are on a circular molecule
     * @return
     */
    public boolean isIntegerInside(int integer) {
        return (integer >= this.getStartPos() && integer <= this.getEndPos());
    }


    /**
     * Checks if <code>Position</code> pos1 is completely inside this <code>Position</code>.
     *
     * @param pos1
     * @param pos2
     * @return
     */
    public boolean isPositionInside(Position pos1) {
        int pos1_start = pos1.getStartPos();
        int pos1_end = pos1.getEndPos();
        int pos2_start = this.getStartPos();
        int pos2_end = this.getEndPos();

        return (pos1_start >= pos2_start && pos1_start <= pos2_end && pos1_end <= pos2_end && pos1_end >= pos2_start);
    }

    public boolean isSinglePos() {
        return singlePos;
    }

    public boolean isReverse() {
        return complement;
    }


    public int getStartPos() {
        return startingPos;
    }

    public void setStartPos(int start) {
        startingPos = start;
        singlePos = (startingPos == endingPos);
    }

    public void setEndPos(int end) {
        endingPos = end;
        singlePos = (startingPos == endingPos);
    }

    public int getEndPos() {
        if (singlePos)
            return startingPos;
        else
            return endingPos;
    }

    public boolean equals(Position pos) {
        return (this.startingPos == pos.startingPos) && (this.endingPos == pos.endingPos) && (this.complement == pos.complement);
    }

    public Object clone() throws CloneNotSupportedException {
        Position cloned = (Position) super.clone();
        cloned.startingPos = startingPos;
        cloned.endingPos = endingPos;
        cloned.complement = complement;

        return cloned;
    }

}
