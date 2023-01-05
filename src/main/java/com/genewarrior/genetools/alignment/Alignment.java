/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.alignment;

import com.genewarrior.genetools.sequenceHandling.Aminoacids;
import com.genewarrior.genetools.sequenceHandling.Nucleotide;

/**
 * @author btserver
 */
public class Alignment {
    public byte alignmenttype = 0;
    public byte[] a;
    public byte[] b;
    public SubstitutionMatrix sm;
    public int score;
    /**
     * startPos: position at which the alignment starts (in longer sequence a)
     */
    public int startPosA;
    public int startPosB;
    /**
     * endPos: position at which the alignment ends (in longer sequence a)
     */
    public int endPosA;
    public int endPosB;

    /**
     * returns a boolean array of matches/non-matches
     * (matching in the sense of N matches A, etc.)
     * A match to a gap is false
     *
     * @return boolean[]
     */
    public boolean[] getMatching() {
        boolean[] matching = new boolean[a.length];

        if (sm.type == SubstitutionMatrix.AA) {
            for (int i = 0; i < a.length; i++)
                matching[i] = Aminoacids.compareAminoacids(a[i], b[i]);
        } else {
            for (int i = 0; i < a.length; i++)
                matching[i] = Nucleotide.compareNucleotide(a[i], b[i]);
        }

        return matching;
    }

    /**
     * returns the number of non-matching positions in the alignment
     *
     * @return
     */
    public int getNonMatchingCount() {
        int nonMatching = 0;

        if (alignmenttype == AlignmentTools.CFE) {
            int start = (startPosA > startPosB ? startPosA : startPosB);
            int end = start + ((endPosA - startPosA) > (endPosB - startPosB) ? endPosA - startPosA : endPosB - startPosB);

            if (sm.type == SubstitutionMatrix.AA) {
                for (int i = start; i <= end; i++)
                    if (!Aminoacids.compareAminoacids(a[i], b[i]))
                        nonMatching++;
            } else //DNA
            {
                for (int i = start; i <= end; i++)
                    if (!Nucleotide.compareNucleotide(a[i], b[i]))
                        nonMatching++;
            }
        } else {
            if (sm.type == SubstitutionMatrix.AA) {
                for (int i = 0; i < a.length; i++)
                    if (!Aminoacids.compareAminoacids(a[i], b[i]))
                        nonMatching++;
            } else {
                for (int i = 0; i < a.length; i++)
                    if (!Nucleotide.compareNucleotide(a[i], b[i]))
                        nonMatching++;
            }
        }

        return nonMatching;
    }


    /**
     * Calculates p-value of this alignment. !!! only for blosum62
     *
     * @return
     */
    public double getPValue(int length_m, int length_n) {
        if (!sm.name.equalsIgnoreCase("blosum") || sm.distance != 62)
            return 999;

        double lambda = 0.252F;
        double K = 0.035F;
        return 1 - Math.exp(-1 * K * (double) length_m * (double) length_n * Math.exp(-1 * lambda * (double) score));
    }
}
