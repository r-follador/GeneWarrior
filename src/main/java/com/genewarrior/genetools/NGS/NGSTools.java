/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.genewarrior.genetools.NGS;


import com.genewarrior.genetools.sequenceHandling.Position;

/**
 * @author Rainer
 */
public class NGSTools {

    public static QualitySequenceDNA getSubsequence(QualitySequenceDNA qs, int start, int stop) {
        byte[] qual = new byte[stop - start + 1];
        System.arraycopy(qs.getQuality(), start, qual, 0, stop - start + 1);
        return new QualitySequenceDNA(qs.getName(), qs.getSequence().getLinearSubSequence(new Position(start, stop, false)), qual);
    }


}
