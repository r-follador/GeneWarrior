/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.sequenceHandling;


import com.genewarrior.genetools.alignment.*;

import java.util.ArrayList;

/**
 * Toolkit to handle <code>SequenceDNA</code>, do not directly access <code>SequenceDNA</code> methods.
 *
 * @author kingcarlxx
 */
public class SequenceTools {
    final static Nucleotide nucl = new Nucleotide();
    final static Aminoacids aminos = new Aminoacids();

    /**
     * Replaces an aminoacid/nt in the given Sequence
     *
     * @param seq
     * @param replace replace this aa/nt
     * @param by      by this aa/nt
     */
    static public void replaceLetter(Sequence seq, byte replace, byte by) {
        byte[] bs = seq.getByteArray();
        for (int i = 0; i < bs.length; i++) {
            if (bs[i] == replace)
                bs[i] = by;
        }
    }

    /**
     * Trims the prefix-Sequence from the 5'-end of the seq2trim-Sequence if it has less than maxMistakes and occurs within lookInFirstBp of the 5'-end
     *
     * @param prefix
     * @param seq2trim
     * @param maxMistakes
     * @param lookInFirstBp
     * @return trimmed sequence or null if nothing is trimmed
     */
    static public SequenceDNA trim5seq(SequenceDNA prefix, SequenceDNA seq2trim, int maxMistakes, int lookInFirstBp) {
        lookInFirstBp--;
        if (lookInFirstBp > seq2trim.getLength() - 1)
            lookInFirstBp = seq2trim.getLength() - 1;
        SubstitutionMatrix sms = (new SubstitutionMatricesDNA()).dna;
        Alignment align = AlignmentTools.align(prefix, seq2trim.getLinearSubSequence(new Position(0, lookInFirstBp, false)), sms, AlignmentTools.CFE);
        int mismatches = align.getNonMatchingCount();//prefix.getLength()-(align.endPosA-align.startPosA+1)+align.getNonMatchingCount();

        if (mismatches > maxMistakes)
            return null;
        if ((align.a.length - lookInFirstBp) > maxMistakes - mismatches + 1) {
            return null;
        }

        int startSOI = align.endPosB + 1;

        return seq2trim.getLinearSubSequence(new Position(startSOI, seq2trim.getLength() - 1, false));
    }

    /**
     * Trims the prefix-Sequence from the 5'-end of the seq2trim-Sequence if it has less than maxMistakes and occurs within lookInFirstBp of the 5'-end
     *
     * @param prefix
     * @param seq2trim
     * @param maxMistakes
     * @param lookInFirstBp
     * @return trimmed sequence or null if nothing is trimmed
     */
    static public SequenceAA trim5seq(SequenceAA prefix, SequenceAA seq2trim, int maxMistakes, int lookInFirstBp) {
        lookInFirstBp--;
        if (lookInFirstBp > seq2trim.getLength() - 1)
            lookInFirstBp = seq2trim.getLength() - 1;
        SubstitutionMatrix sms = (new SubstitutionMatrices()).blosum[6];
        Alignment align = AlignmentTools.align(prefix, seq2trim.getLinearSubSequence(0, lookInFirstBp), sms, AlignmentTools.CFE);
        int mismatches = align.getNonMatchingCount();//prefix.getLength()-(align.endPosA-align.startPosA+1)+align.getNonMatchingCount();

        if (mismatches > maxMistakes)
            return null;

        if ((align.a.length - lookInFirstBp) > maxMistakes - mismatches + 1) {
            return null;
        }

        int startSOI = align.endPosB + 1;

        return seq2trim.getLinearSubSequence(startSOI, seq2trim.getLength() - 1);
    }

    /**
     * Trims the prefix-Sequence from the 3'-end of the seq2trim-Sequence if it has less than maxMistakes and the matching part of the sequence is at least minRecognizedLength and it occurs in the last bp (on the 3'-End) which is indicated by lookInLastBp
     * used to trim primers from sequencing reads, where a primer might only be partially sequenced
     *
     * @param suffix
     * @param seq2trim
     * @param maxMistakes
     * @param minRecognizedLength
     * @param lookInLastBp
     * @return trimmed sequence or null if nothing is trimmed
     */
    static public SequenceDNA trim3seq(SequenceDNA suffix, SequenceDNA seq2trim, int maxMistakes, int minRecognizedLength, int lookInLastBp) {
        if (lookInLastBp > seq2trim.getLength() - 1)
            lookInLastBp = seq2trim.getLength() - 1;
        int lookAfterPos = seq2trim.getLength() - lookInLastBp;
        SubstitutionMatrix sms = (new SubstitutionMatricesDNA()).dna;
        Alignment align = AlignmentTools.align(suffix, seq2trim.getLinearSubSequence(new Position(lookAfterPos, seq2trim.getLength() - 1, false)), sms, AlignmentTools.CFE);
        int mismatches = align.getNonMatchingCount();
        int recognizedLength = align.endPosA - align.startPosA + 1;

        if (mismatches > maxMistakes)
            return null;
        if (minRecognizedLength > recognizedLength)
            return null;

        if (align.startPosA > 0)
            return null;

        int endSOI = lookAfterPos + align.startPosB - 1;

        return seq2trim.getLinearSubSequence(new Position(0, endSOI, false));
    }

    /**
     * Trims the prefix-Sequence from the 3'-end of the seq2trim-Sequence if it has less than maxMistakes and occurs within lookInLastBp of the 3'-end
     *
     * @param suffix
     * @param seq2trim
     * @param maxMistakes
     * @param lookInLastBp
     * @return trimmed sequence or null if nothing is trimmed
     */
    static public SequenceAA trim3seq_keep(SequenceAA suffix, SequenceAA seq2trim, int maxMistakes, int lookInLastBp) {
        if (lookInLastBp > seq2trim.getLength() - 1)
            lookInLastBp = seq2trim.getLength() - 1;
        int lookAfterPos = seq2trim.getLength() - lookInLastBp;

        SubstitutionMatrix sms = (new SubstitutionMatrices()).blosum[6];
        Alignment align = AlignmentTools.align(suffix, seq2trim.getLinearSubSequence(lookAfterPos, seq2trim.getLength() - 1), sms, AlignmentTools.CFE);
        int mismatches = align.getNonMatchingCount();


        if (mismatches > maxMistakes)
            return null;

        if ((align.a.length - lookInLastBp) > maxMistakes - mismatches + 1) {
            return null;
        }


        int endSOI = lookAfterPos + align.endPosB;

        return seq2trim.getLinearSubSequence(0, endSOI);
    }

    /**
     * Trims the prefix-Sequence from the 3'-end of the seq2trim-Sequence if it has less than maxMistakes and the matching part of the sequence is at least minRecognizedLength and it occurs in the last bp (on the 3'-End) which is indicated by lookInLastBp
     * used to trim primers from sequencing reads, where a primer might only be partially sequenced
     *
     * @param suffix
     * @param seq2trim
     * @param maxMistakes
     * @param minRecognizedLength
     * @param lookInLastBp
     * @return trimmed sequence or null if nothing is trimmed
     */
    static public SequenceAA trim3seq(SequenceAA suffix, SequenceAA seq2trim, int maxMistakes, int minRecognizedLength, int lookInLastBp) {
        if (lookInLastBp > seq2trim.getLength() - 1)
            lookInLastBp = seq2trim.getLength() - 1;
        int lookAfterPos = seq2trim.getLength() - lookInLastBp;
        SubstitutionMatrix sms = (new SubstitutionMatrices()).blosum[6];
        Alignment align = AlignmentTools.align(suffix, seq2trim.getLinearSubSequence(lookAfterPos, seq2trim.getLength() - 1), sms, AlignmentTools.CFE);
        int mismatches = align.getNonMatchingCount();
        int recognizedLength = align.endPosA - align.startPosA + 1;

        if (mismatches > maxMistakes)
            return null;
        if (minRecognizedLength > recognizedLength)
            return null;

        if (align.startPosA > 0)
            return null;

        int endSOI = lookAfterPos + align.startPosB - 1;

        return seq2trim.getLinearSubSequence(0, endSOI);
    }

    /**
     * Translates given <code>SequenceDNA</code> to <code>SequenceAA</code> using TranslationTable and codonStart
     * Attention: codon with an ambigiuos DNA sequence will be translated to X (Xaa; any amino acid)
     * The first codon will be translated using the special start codon translation table
     *
     * @param seq
     * @param TranslationTable
     * @param codonStart       1,2 or 3
     * @return
     */
    static public SequenceAA DNA2Protein(SequenceDNA seq, int TranslationTable, int codonStart) {
        return DNA2Protein(seq, TranslationTable, codonStart, true);
    }

    /**
     * Translates given <code>SequenceDNA</code> to <code>SequenceAA</code> using TranslationTable and codonStart
     * Attention: codon with an ambigiuos DNA sequence will be translated to X (Xaa; any amino acid)
     *
     * @param seq
     * @param TranslationTable
     * @param codonStart       1,2 or 3
     * @param useStartCodon    use for the first codon the special start codon translation table
     * @return
     */
    static public SequenceAA DNA2Protein(SequenceDNA seq, int TranslationTable, int codonStart, boolean useStartCodon) {
        if (seq == null)
            return null;

        if (seq.getLength() - (codonStart - 1) < 3)
            return null;


        seq.convert2DNA();
        byte[] nucleotidesequence = seq.getByteArray();
        byte[] proteinsequence = new byte[(seq.getLength() - codonStart + 1) / 3];

        int i = codonStart + 1;


        proteinsequence[0] = aminos.translationTable[TranslationTable][(useStartCodon ? 1 : 0)][nucleotidesequence[i - 2]][nucleotidesequence[i - 1]][nucleotidesequence[i]];
        int k = 1;
        for (i += 3; i < seq.getLength(); i += 3) {
            if (aminos.translationTable[TranslationTable][0][nucleotidesequence[i - 2]][nucleotidesequence[i - 1]][nucleotidesequence[i]] == Aminoacids.__)
                proteinsequence[k] = Aminoacids._X;
            else
                proteinsequence[k] = aminos.translationTable[TranslationTable][0][nucleotidesequence[i - 2]][nucleotidesequence[i - 1]][nucleotidesequence[i]];
            k++;
        }

        SequenceAA translatedProtein = new SequenceAA(proteinsequence);
        return translatedProtein;
    }

    /**
     * Searches for Start Codon in the upper strand of given <code>SequenceDNA</code>, using given TranslationTable
     *
     * @param seq
     * @param TranslationTable
     * @return int-array of possible start codons (first position)
     */
    static public int[] findStartCodon(SequenceDNA seq, int TranslationTable) {
        if (seq.getLength() < 3)
            return null;

        ArrayList<Integer> matchPosition = new ArrayList<Integer>();
        byte[] nucleotidesequence = seq.getByteArray();

        for (int i = 2; i < seq.getLength(); i++) {
            if (aminos.translationTable[TranslationTable][1][nucleotidesequence[i - 2]][nucleotidesequence[i - 1]][nucleotidesequence[i]] != Aminoacids.__)
                matchPosition.add(i - 2);
        }

        //convert ArrayList matchPosition to int[] matchPos

        int[] matchPos = new int[matchPosition.size()];


        for (int k = 0; k < matchPos.length; k++) {
            matchPos[k] = matchPosition.get(k);
        }

        return matchPos;
    }

    static public boolean startsWithStartCodon(SequenceDNA seq, int TranslationTable) {
        if (seq.getLength() < 3)
            return false;

        byte[] nucleotidesequence = seq.getByteArray();


        return (aminos.translationTable[TranslationTable][1][nucleotidesequence[0]][nucleotidesequence[1]][nucleotidesequence[2]] == Aminoacids._M);
    }

    static public boolean endsWithEndCodon(SequenceDNA seq, int TranslationTable) {
        if (seq.getLength() < 3)
            return false;

        byte[] nucleotidesequence = seq.getByteArray();
        int length = nucleotidesequence.length;


        return (aminos.translationTable[TranslationTable][0][nucleotidesequence[length - 3]][nucleotidesequence[length - 2]][nucleotidesequence[length - 1]] == Aminoacids._TERM);
    }

    /**
     * Searches for Terminator Codons in the upper strand of given <code>SequenceDNA</code>, using given TranslationTable
     *
     * @param seq
     * @param TranslationTable
     * @return int-array of possible terminator codons (first position)
     */
    static public int[] findTerminatorCodon(SequenceDNA seq, int TranslationTable) {
        if (seq.getLength() < 3)
            return null;

        ArrayList<Integer> matchPosition = new ArrayList<Integer>();
        byte[] nucleotidesequence = seq.getByteArray();

        for (int i = 2; i < seq.getLength(); i++) {
            if (aminos.translationTable[TranslationTable][0][nucleotidesequence[i - 2]][nucleotidesequence[i - 1]][nucleotidesequence[i]] == Aminoacids._TERM)
                matchPosition.add(i - 2);
        }

        //convert ArrayList matchPosition to int[] matchPos

        int[] matchPos = new int[matchPosition.size()];


        for (int k = 0; k < matchPos.length; k++) {
            matchPos[k] = matchPosition.get(k);
        }

        return matchPos;
    }


    /**
     * Returns combination of <code>SequenceDNA</code> first with appended <code>SequenceDNA</code> second.
     *
     * @param first
     * @param second
     * @return
     */
    static public SequenceDNA combineSequences(SequenceDNA first, SequenceDNA second) {
        if (first == null && second == null)
            return null;

        if (first == null)
            return new SequenceDNA(second.getByteArray(), second.isRNA());

        if (second == null)
            return new SequenceDNA(first.getByteArray(), first.isRNA());

        boolean isRNA = first.isRNA();
        if (second.isRNA() != isRNA) {
            if (isRNA)
                second.convert2RNA();
            else
                second.convert2DNA();
        }


        byte[] combinedSequence = new byte[first.getLength() + second.getLength()];

        System.arraycopy(first.getByteArray(), 0, combinedSequence, 0, first.getLength());
        System.arraycopy(second.getByteArray(), 0, combinedSequence, first.getLength(), second.getLength());

        SequenceDNA combined = new SequenceDNA(combinedSequence, isRNA);

        return combined;

    }

    /**
     * Get the reverse or the complement or both of the passed <code>SequenceDNA</code>
     *
     * @param seq        the sequence to convert
     * @param reverse    set to true if you want the reverse sequence
     * @param complement set to true if you want the complement (lower strand) sequence
     * @return
     */
    static public SequenceDNA reverseComplement(SequenceDNA seq, boolean reverse, boolean complement) {
        byte[] convertedSeq = seq.getByteArray();


        if (complement)
            convertedSeq = nucl.complement(convertedSeq, seq.isRNA());
        if (reverse) //todo: find better way to reverse
        {
            int left = 0;
            int right = convertedSeq.length - 1;

            while (left < right) {
                byte temp = convertedSeq[left];
                convertedSeq[left] = convertedSeq[right];
                convertedSeq[right] = temp;

                left++;
                right--;
            }
        }

        return new SequenceDNA(convertedSeq, seq.isRNA());

    }

    /**
     * Checks given String for invalid nucleotides
     *
     * @param seq2check String to check
     * @return index array (int[]) with invalid positions
     */
    static public int[] invalidPositionsDNA(String seq2check) {
        return nucl.returnInvalidPositions(seq2check);
    }

    /**
     * Checks given String for invalid aminoacids
     *
     * @param seq2check String to check
     * @return index array (int[]) with invalid positions
     */
    static public int[] invalidPositionsProtein(String seq2check) {
        return aminos.returnInvalidPositions(seq2check);
    }

    static public double getGCcontent(SequenceDNA seq) {
        int[] count = seq.getNucleotideCount();
        int GCcount = count[Nucleotide._G] + count[Nucleotide._C] + count[Nucleotide._S];

        return (double) GCcount / (double) (seq.getLength() - count[Nucleotide.__]);
    }

    /**
     * Search for Sequence in a longer Sequence. Ambigious nucleotides are extended, but only in the motiv (sought for sequence). Searches only in the upper strand (not in the compliment sequence)
     *
     * @param searchFor <code>SequenceDNA</code> which is sought for
     * @param searchIn  <code>SequenceDNA</code> which is searched
     * @return <code>int[]</code> index of first nucleotid of found sequence in <code>searchIn</code>; returns null if nothing found
     */
    static public int[] findSequenceUpperStrand(SequenceDNA searchFor, SequenceDNA searchIn) {
        if (searchFor.getLength() > searchIn.getLength())
            return null;

        if (searchFor.getLength() == 0 || searchIn.getLength() == 0)
            return null;


        ArrayList<Integer> matchPosition = new ArrayList<Integer>();
        byte[] muster = searchFor.getByteArray();
        int musterlength = muster.length;
        byte[] string = searchIn.getByteArray();
        int stringlength = string.length;


        //Horspool-Algorithmus; see 
        //Todo: ignore gaps

        int[] occ = new int[Nucleotide.alphabetSize];

        for (int i = 0; i < Nucleotide.alphabetSize; i++)
            occ[i] = -1;

        for (int i = 0; i < musterlength - 1; i++) //letzter buchstabe des musters wird nicht berücksichtigt
        {
            occ[muster[i]] = i;
        }

        int i = 0;
        int j;

        while (i <= stringlength - musterlength) {
            j = musterlength - 1;
            while (j >= 0 && Nucleotide.compareNucleotide(muster[j], string[i + j]))
                j--;
            if (j < 0)
                matchPosition.add(i);


            i += musterlength - 1;
            i -= occ[string[i]];

        }

        //convert ArrayList matchPosition to int[] matchPos

        int[] matchPos = new int[matchPosition.size()];


        for (int k = 0; k < matchPos.length; k++) {
            matchPos[k] = matchPosition.get(k);
        }

        return matchPos;

    }

    /**
     * Same as <code>findSequenceUpperStrand()</code>, but searches in complement sequence.
     *
     * @param searchFor <code>SequenceDNA</code> which is sought for
     * @param searchIn  <code>SequenceDNA</code> which's complement is searched
     * @return <code>int[]</code> index of first nucleotid of found sequence in <code>searchIn</code>; returns null if nothing found
     */
    static public int[] findSequenceLowerStrand(SequenceDNA searchFor, SequenceDNA searchIn) {
        searchFor = SequenceTools.reverseComplement(searchFor, true, true);
        return findSequenceUpperStrand(searchFor, searchIn);
    }

    /**
     * Search for Protein-Sequence in a longer Protein-Sequence. Ambigious aminoacids are extended, but only in the motiv (sought for sequence).
     *
     * @param searchFor <code>SequenceAA</code> which is sought for
     * @param searchIn  <code>SequenceAA</code> which is searched
     * @return <code>int[]</code> index of first aminoacids of found sequence in <code>searchIn</code>; returns null if nothing found
     */
    static public int[] findSequenceProtein(SequenceAA searchFor, SequenceAA searchIn) {
        if (searchFor.getLength() > searchIn.getLength())
            return null;

        if (searchFor.getLength() == 0 || searchIn.getLength() == 0)
            return null;


        ArrayList<Integer> matchPosition = new ArrayList<Integer>();
        byte[] muster = searchFor.getByteArray();
        int musterlength = muster.length;
        byte[] string = searchIn.getByteArray();
        int stringlength = string.length;


        //Horspool-Algorithmus; see
        //Todo: ignore gaps

        int[] occ = new int[Aminoacids.alphabetSize];

        for (int i = 0; i < Aminoacids.alphabetSize; i++)
            occ[i] = -1;

        for (int i = 0; i < musterlength - 1; i++) //letzter buchstabe des musters wird nicht berücksichtigt
        {
            occ[muster[i]] = i;
        }

        int i = 0;
        int j;

        while (i <= stringlength - musterlength) {
            j = musterlength - 1;
            while (j >= 0 && Aminoacids.compareAminoacids(muster[j], string[i + j]))
                j--;
            if (j < 0)
                matchPosition.add(i);


            i += musterlength - 1;
            i -= occ[string[i]];

        }

        //convert ArrayList matchPosition to int[] matchPos

        int[] matchPos = new int[matchPosition.size()];


        for (int k = 0; k < matchPos.length; k++) {
            matchPos[k] = matchPosition.get(k);
        }

        return matchPos;

    }

}
