/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.alignment;

import com.genewarrior.genetools.Contig;
import com.genewarrior.genetools.Feature;
import com.genewarrior.genetools.sequenceHandling.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author btserver
 */
public class AlignmentTools {
    public static final byte Local = 1; //Smith-Waterman
    public static final byte Global = 2; //Needleman-Wunsch
    public static final byte CFE = 3; //Cost free ends

    public static int overlapSize(Alignment align) {
        if (align.alignmenttype == Global) {
            return align.a.length;
        }

        if (align.alignmenttype == Local) {
            return align.a.length;
        }

        if (align.alignmenttype == CFE) {
            int start = 0;
            int end = align.a.length - 1;
            int start1 = 0;
            while (align.a[start1] == 0 && align.b[start1] != 0)
                start1++;
            int start2 = 0;
            while (align.a[start2] != 0 && align.b[start2] == 0)
                start2++;
            start = (start1 > start2 ? start1 : start2);

            int end1 = align.a.length - 1;
            while (align.a[end1] == 0 && align.b[end1] != 0)
                end1--;
            int end2 = align.a.length - 1;
            while (align.a[end2] != 0 && align.b[end2] == 0)
                end2--;
            end = (end1 < end2 ? end1 : end2);

            return end - start;
        }
        return 0;


    }

    /**
     * Returns the percentage of exact matches in an alignment
     *
     * @param align
     * @return
     */
    public static float percentageExactMatches(Alignment align) {
        Nucleotide nucl = new Nucleotide();
        Aminoacids aminos = new Aminoacids();

        int match = 0;
        int total = 0;

        int start = 0;
        int end = align.a.length - 1;
        if (align.alignmenttype == CFE) {
            int start1 = 0;
            while (align.a[start1] == 0 && align.b[start1] != 0)
                start1++;
            int start2 = 0;
            while (align.a[start2] != 0 && align.b[start2] == 0)
                start2++;
            start = (start1 > start2 ? start1 : start2);

            int end1 = align.a.length - 1;
            while (align.a[end1] == 0 && align.b[end1] != 0)
                end1--;
            int end2 = align.a.length - 1;
            while (align.a[end2] != 0 && align.b[end2] == 0)
                end2--;
            end = (end1 < end2 ? end1 : end2);
        }

        for (int i = start; i < end + 1; i++) {
            total++;
            if (align.a[i] == align.b[i]) {
                if (align.sm.type == SubstitutionMatrix.AA)
                    if (Aminoacids.compareAminoacids(align.a[i], align.b[i]))
                        match++;
                if (align.sm.type == SubstitutionMatrix.DNA)
                    if (Nucleotide.compareNucleotide(align.a[i], align.b[i]))
                        match++;
            }
        }

        return ((float) match / (float) total) * 100F;
    }

    /**
     * Quick align the query aa to the features in the ArrayList search. Orders according to descending score
     *
     * @param query
     * @param search ArrayList of features
     * @param sm     SubstitutionMatrix
     * @param type   Local, Global, CFE
     * @return an ArrayList of type Rank, order according to descending score. Use rank.rank for the score, and rank.seq for the feature
     */
    public static ArrayList<Rank> scoreAgainstList(SequenceAA query, ArrayList<Feature> search, SubstitutionMatrix sm, byte type) {
        ArrayList<Rank> ranking = new ArrayList<Rank>();
        for (Feature seq : search) {
            if (seq.isProteinProduct()) {
                int score = quickalign(query, seq.getProteinProduct().getSequence().killGaps(), sm, type);
                ranking.add(new Rank(score, seq));
            }
        }

        Collections.sort(ranking);
        Collections.reverse(ranking);

        return ranking;
    }

    /**
     * Quick align the query dna to the features in the ArrayList search. Orders according to descending score
     *
     * @param query
     * @param search ArrayList of features
     * @param sm     SubstitutionMatrix
     * @param type   Local, Global, CFE
     * @return an ArrayList of type Rank, order according to descending score. Use rank.rank for the score, and rank.seq for the feature
     */
    public static ArrayList<Rank> scoreAgainstListOfFeatures(SequenceDNA query, ArrayList<Feature> search, SubstitutionMatrix sm, byte type) {
        ArrayList<Rank> ranking = new ArrayList<Rank>();
        for (Feature seq : search) {
            if (seq.isProteinProduct()) {
                int score = quickalign(query, ((Contig) seq.getParent()).getSequence().getLinearSubSequence(seq.position), sm, type);
                ranking.add(new Rank(score, seq));
            }
        }

        Collections.sort(ranking);
        Collections.reverse(ranking);

        return ranking;
    }

    /**
     * Quick align the query dna to an Array of <code>NamedSequenceDNA</code> in the ArrayList search. Orders according to descending score
     *
     * @param query
     * @param search ArrayList of features
     * @param sm     SubstitutionMatrix
     * @param type   Local, Global, CFE
     * @return an ArrayList of type Rank, order according to descending score. Use rank.rank for the score, and rank.seq for the feature
     */
    public static ArrayList<Rank> scoreAgainstNamedSequenceDNA(SequenceDNA query, ArrayList<NamedSequenceDNA> search, SubstitutionMatrix sm, byte type) {
        ArrayList<Rank> ranking = new ArrayList<Rank>();
        for (NamedSequenceDNA seq : search) {
            int score = quickalign(query, seq.getSequence(), sm, type);
            ranking.add(new Rank(score, seq));
        }

        Collections.sort(ranking);
        Collections.reverse(ranking);

        return ranking;
    }

    /**
     * Quick align the query dna to an Array of <code>NamedSequenceDNA</code> in the ArrayList search. Orders according to descending score
     *
     * @param query
     * @param search ArrayList of features
     * @param sm     SubstitutionMatrix
     * @param type   Local, Global, CFE
     * @return an ArrayList of type Rank, order according to descending score. Use rank.rank for the score, and rank.seq for the feature
     */
    public static ArrayList<Rank> scoreAgainstNamedSequenceAA(SequenceAA query, ArrayList<NamedSequenceAA> search, SubstitutionMatrix sm, byte type) {
        ArrayList<Rank> ranking = new ArrayList<Rank>();
        for (NamedSequenceAA seq : search) {
            int score = quickalign(query, seq.getSequence(), sm, type);
            ranking.add(new Rank(score, seq));
        }

        Collections.sort(ranking);
        Collections.reverse(ranking);

        return ranking;
    }

    /**
     * Align two SequenceDNA
     *
     * @param a    first sequence
     * @param b    second sequence
     * @param sm   <code>SubstitutionMatrix</code>
     * @param type Local for Smith-Waterman, Global for Needleman-Wunsch, CFE for Cost-free ends
     * @return <code>Alignment</code> containing the score and the alignment
     */
    public static Alignment align(SequenceDNA a, SequenceDNA b, SubstitutionMatrix sm, byte type) {
        if (sm.type != SubstitutionMatrix.DNA) {
            System.out.println("Must be a DNA-SubstitutionMatrix");
            return null;
        }
        byte[] seq1 = a.getByteArray();
        byte[] seq2 = b.getByteArray();

        if (type == Global)
            return globalAlign(seq1, seq2, sm);
        else if (type == Local)
            return localAlign(seq1, seq2, sm);
        else if (type == CFE)
            return cfeAlign(seq1, seq2, sm);
        else
            return null;
    }

    /**
     * Align two SequenceAA
     *
     * @param a    first sequence
     * @param b    second sequence
     * @param sm   <code>SubstitutionMatrix</code>
     * @param type Local for Smith-Waterman, Global for Needleman-Wunsch, CFE for Cost-free ends
     * @return <code>Alignment</code> containing the score and the alignment
     */
    public static Alignment align(SequenceAA a, SequenceAA b, SubstitutionMatrix sm, byte type) {
        if (sm.type != SubstitutionMatrix.AA) {
            System.out.println("Must be a AA-SubstitutionMatrix");
            return null;
        }
        byte[] seq1 = a.getByteArray();
        byte[] seq2 = b.getByteArray();

        if (type == Global)
            return globalAlign(seq1, seq2, sm);
        else if (type == Local)
            return localAlign(seq1, seq2, sm);
        else if (type == CFE)
            return cfeAlign(seq1, seq2, sm);
        else
            return null;
    }

    /**
     * Get the score of an alignment of two SequenceAA (faster, no backtracking)
     *
     * @param a    first sequence
     * @param b    second sequence
     * @param sm   <code>SubstitutionMatrix</code>
     * @param type Local for Smith-Waterman, Global for Needleman-Wunsch, CFE for Cost-free ends
     * @return Score of the optimal alignment
     */
    public static int quickalign(SequenceAA a, SequenceAA b, SubstitutionMatrix sm, byte type) {
        if (sm.type != SubstitutionMatrix.AA) {
            System.out.println("Must be a AA-SubstitutionMatrix");
            return 0;
        }
        byte[] seq1 = (a.getLength() >= b.getLength()) ? a.getByteArray() : b.getByteArray(); //the longer sequence
        byte[] seq2 = (a.getLength() < b.getLength()) ? a.getByteArray() : b.getByteArray();

        if (type == Global)
            return quickglobalAlign(seq1, seq2, sm);
        else if (type == Local)
            return quicklocalAlign(seq1, seq2, sm);
        else if (type == CFE)
            return quickcfeAlign(seq1, seq2, sm);
        else
            return 0;
    }

    /**
     * Get the score of an alignment of two SequenceAA (faster, no backtracking)
     *
     * @param a    first sequence
     * @param b    second sequence
     * @param sm   <code>SubstitutionMatrix</code>
     * @param type Local for Smith-Waterman, Global for Needleman-Wunsch, CFE for Cost-free ends
     * @return Score of the optimal alignment
     */
    public static int quickalign(SequenceDNA a, SequenceDNA b, SubstitutionMatrix sm, byte type) {
        if (sm.type != SubstitutionMatrix.DNA) {
            System.out.println("Must be a DNA-SubstitutionMatrix");
            return 0;
        }
        byte[] seq1 = (a.getLength() >= b.getLength()) ? a.getByteArray() : b.getByteArray(); //the longer sequence
        byte[] seq2 = (a.getLength() < b.getLength()) ? a.getByteArray() : b.getByteArray();

        if (type == Global)
            return quickglobalAlign(seq1, seq2, sm);
        else if (type == Local)
            return quicklocalAlign(seq1, seq2, sm);
        else if (type == CFE)
            return quickcfeAlign(seq1, seq2, sm);
        else
            return 0;
    }


    //Cost-free ends
    private static Alignment cfeAlign(byte[] a, byte[] b, SubstitutionMatrix sm) {
        int a_length = a.length + 1; //add one position, because first row/column is gap
        int b_length = b.length + 1;
        int[][] col = new int[a_length][b_length];

        for (int i = 0; i < b_length; i++) //initialize first col
            col[0][i] = 0;
        for (int j = 0; j < a_length; j++) //initialize first row
            col[j][0] = 0;

        for (int j = 1; j < a_length; j++) //each col
        {
            for (int i = 1; i < b_length; i++) //each row
            {
                int gapabove = col[j][i - 1] + sm.gapcost;
                int gapleft = col[j - 1][i] + sm.gapcost;
                int match = col[j - 1][i - 1] + sm.matrix[a[j - 1]][b[i - 1]];
                col[j][i] = max(gapabove, gapleft, match);
            }
        }

        int max_i = 0; //position of max score
        int max_j = 0; //position of max score
        int max_score = 0;
        for (int j = 1; j < a_length; j++) //look for highest score in last column
        {
            if (max_score < col[j][b_length - 1]) {
                max_score = col[j][b_length - 1];
                max_i = b_length - 1;
                max_j = j;
            }
        }
        for (int i = 1; i < b_length; i++) //look for highest score in last row
        {
            if (max_score < col[a_length - 1][i]) {
                max_score = col[a_length - 1][i];
                max_i = i;
                max_j = a_length - 1;
            }
        }

        int Score = max_score;

        Alignment align = new Alignment();
        align.alignmenttype = CFE;
        align.score = Score;
        align.endPosA = max_j - 1;
        align.endPosB = max_i - 1;
        align.sm = sm;

        //System.out.println(Arrays.deepToString(col));

        //Backtracking
        byte[] a_align = new byte[a_length + b_length];
        byte[] b_align = new byte[a_length + b_length];

        int j = max_j; //cols
        int i = max_i; //rows
        int pos = a_align.length - 1; //writing position

        if (max_j < a_length - 1) //add free gaps to b
        {
            for (int j_ = a_length - 1; j_ > max_j; j_--) {
                a_align[pos] = a[j_ - 1];
                b_align[pos] = 0;
                pos--;
            }
        }

        if (max_i < b_length - 1) //add free gaps to a
        {
            for (int i_ = b_length - 1; i_ > max_i; i_--) {
                b_align[pos] = b[i_ - 1];
                a_align[pos] = 0;
                pos--;
            }
        }

        while (j > 0 && i > 0) {
            int gap = col[j][i] - sm.gapcost;
            int match = Score - sm.matrix[a[j - 1]][b[i - 1]];

            if (j > 0 && i > 0 && col[j - 1][i - 1] == match) //Match
            {
                //System.out.println("j:"+j+"i:"+i+"; Match ("+a[j-1]+","+b[i-1]+")");
                a_align[pos] = a[j - 1];
                b_align[pos] = b[i - 1];
                pos--;
                j--;
                i--;
                Score = col[j][i];

            } else if (j > 0 && col[j - 1][i] == gap) //Gap left
            {
                //System.out.println("j:"+(j-1)+"i:"+i+"; Gap in b");
                a_align[pos] = a[j - 1];
                b_align[pos] = 0;
                pos--;
                j--;
                Score = col[j][i];
            } else if (i > 0 && col[j][i - 1] == gap) //Gap above
            {
                //System.out.println("j:"+(j)+"i:"+(i-1)+"; Gap in a");
                a_align[pos] = 0;
                b_align[pos] = b[i - 1];
                pos--;
                i--;
                Score = col[j][i];
            } else {
                System.out.println("alignment-backtrack error, shouldn't happen, i: " + i + ";j:" + j);
                break;
            }
        } //end while

        align.startPosB = i;

        while (i > 0) //insert gaps in a, copy rest of b
        {
            a_align[pos] = 0;
            b_align[pos] = b[i - 1];
            pos--;
            i--;
        }

        align.startPosA = j;

        while (j > 0) //insert gaps in b, copy rest of a
        {
            b_align[pos] = 0;
            a_align[pos] = a[j - 1];
            pos--;
            j--;
        }

        align.a = Arrays.copyOfRange(a_align, pos + 1, a_align.length);
        align.b = Arrays.copyOfRange(b_align, pos + 1, b_align.length);

        return align;
    }

    private static Alignment localAlign(byte[] a, byte[] b, SubstitutionMatrix sm) {
        int a_length = a.length + 1; //add one position, because first row/column is gap
        int b_length = b.length + 1;
        int[][] col = new int[a_length][b_length];

        for (int i = 0; i < b_length; i++) //initialize first col
            col[0][i] = 0;
        for (int j = 0; j < a_length; j++) //initialize first row
            col[j][0] = 0;

        int max_i = 0; //position of max score
        int max_j = 0; //position of max score
        int max_score = 0; //value of max_score
        for (int j = 1; j < a_length; j++) //each col
        {
            for (int i = 1; i < b_length; i++) //each row
            {
                int gapabove = col[j][i - 1] + sm.gapcost;
                int gapleft = col[j - 1][i] + sm.gapcost;
                int match = col[j - 1][i - 1] + sm.matrix[a[j - 1]][b[i - 1]];
                col[j][i] = max(gapabove, gapleft, match, 0);
                if (max_score < col[j][i]) //new max score
                {
                    max_score = col[j][i];
                    max_i = i;
                    max_j = j;
                }
            }
        }
        int Score = max_score;

        Alignment align = new Alignment();
        align.alignmenttype = Local;
        align.score = Score; //score is last entry
        align.endPosA = max_j - 1;
        align.endPosB = max_i - 1;
        align.sm = sm;

        //System.out.println(Arrays.deepToString(col));

        //Backtracking
        byte[] a_align = new byte[max_i + max_j];
        byte[] b_align = new byte[max_i + max_j];

        int j = max_j; //cols
        int i = max_i; //rows
        int pos = a_align.length - 1; //writing position


        while (j > 0 && i > 0) {
            int gap = col[j][i] - sm.gapcost;
            int match = Score - sm.matrix[a[j - 1]][b[i - 1]];
            align.startPosA = j - 1;
            align.startPosB = i - 1;

            if (j > 0 && i > 0 && col[j - 1][i - 1] == match) //Match
            {
                //System.out.println("j:"+j+"i:"+i+"; Match ("+a[j-1]+","+b[i-1]+")");
                a_align[pos] = a[j - 1];
                b_align[pos] = b[i - 1];
                pos--;
                j--;
                i--;
                Score = col[j][i];

            } else if (j > 0 && col[j - 1][i] == gap) //Gap left
            {
                //System.out.println("j:"+(j-1)+"i:"+i+"; Gap in b");
                a_align[pos] = a[j - 1];
                b_align[pos] = 0;
                pos--;
                j--;
                Score = col[j][i];
            } else if (i > 0 && col[j][i - 1] == gap) //Gap above
            {
                //System.out.println("j:"+(j)+"i:"+(i-1)+"; Gap in a");
                a_align[pos] = 0;
                b_align[pos] = b[i - 1];
                pos--;
                i--;
                Score = col[j][i];
            } else {
                System.out.println("alignment-backtrack error, shouldn't happen, i: " + i + ";j:" + j);
                break;
            }

            if (Score == 0)
                break;
        } //end while


        align.a = Arrays.copyOfRange(a_align, pos + 1, a_align.length);
        align.b = Arrays.copyOfRange(b_align, pos + 1, b_align.length);

        return align;
    }

    private static Alignment globalAlign(byte[] a, byte[] b, SubstitutionMatrix sm) {
        int a_length = a.length + 1; //add one position, because first row/column is gap
        int b_length = b.length + 1;
        int[][] col = new int[a_length][b_length];

        for (int i = 0; i < b_length; i++) //initialize first col
            col[0][i] = i * sm.gapcost;
        for (int j = 0; j < a_length; j++) //initialize first row
            col[j][0] = j * sm.gapcost;

        for (int j = 1; j < a_length; j++) //each col
        {
            for (int i = 1; i < b_length; i++) //each row
            {
                int gapabove = col[j][i - 1] + sm.gapcost;
                int gapleft = col[j - 1][i] + sm.gapcost;
                int match = col[j - 1][i - 1] + sm.matrix[a[j - 1]][b[i - 1]];
                col[j][i] = max(gapabove, gapleft, match);
            }
        }
        int Score = col[a_length - 1][b_length - 1];

        Alignment align = new Alignment();
        align.alignmenttype = Global;
        align.score = Score; //score is last entry
        align.startPosA = 0;
        align.endPosA = a_length - 2; //global alignment goes from start to end
        align.startPosB = 0;
        align.endPosB = b_length - 2;
        align.sm = sm;

        //System.out.println(Arrays.deepToString(col));

        //Backtracking
        byte[] a_align = new byte[a_length + b_length];
        byte[] b_align = new byte[a_length + b_length];

        int j = a_length - 1; //cols
        int i = b_length - 1; //rows
        int pos = a_align.length - 1; //writing position


        while (j > 0 && i > 0) {
            int gap = col[j][i] - sm.gapcost;
            int match = Score - sm.matrix[a[j - 1]][b[i - 1]];

            if (j > 0 && i > 0 && col[j - 1][i - 1] == match) //Match
            {
                //System.out.println("j:"+j+"i:"+i+"; Match ("+a[j-1]+","+b[i-1]+")");
                a_align[pos] = a[j - 1];
                b_align[pos] = b[i - 1];
                pos--;
                j--;
                i--;
                Score = col[j][i];

            } else if (j > 0 && col[j - 1][i] == gap) //Gap left
            {
                //System.out.println("j:"+(j-1)+"i:"+i+"; Gap in b");
                a_align[pos] = a[j - 1];
                b_align[pos] = 0;
                pos--;
                j--;
                Score = col[j][i];
            } else if (i > 0 && col[j][i - 1] == gap) //Gap above
            {
                //System.out.println("j:"+(j)+"i:"+(i-1)+"; Gap in a");
                a_align[pos] = 0;
                b_align[pos] = b[i - 1];
                pos--;
                i--;
                Score = col[j][i];
            } else {
                System.out.println("alignment-backtrack error, shouldn't happen, i: " + i + ";j:" + j);
                break;
            }
        } //end while
        while (i > 0) //insert gaps in a, copy rest of b
        {
            a_align[pos] = 0;
            b_align[pos] = b[i - 1];
            pos--;
            i--;
        }
        while (j > 0) //insert gaps in b, copy rest of a
        {
            b_align[pos] = 0;
            a_align[pos] = a[j - 1];
            pos--;
            j--;
        }

        align.a = Arrays.copyOfRange(a_align, pos + 1, a_align.length);
        align.b = Arrays.copyOfRange(b_align, pos + 1, b_align.length);

        return align;
    }

    private static int quickglobalAlign(byte[] a, byte[] b, SubstitutionMatrix sm) {
        int a_length = a.length + 1; //add one position, because first row/column is gap
        int b_length = b.length + 1;
        int[] col1 = new int[b_length];
        int[] col2 = new int[b_length];

        for (int i = 0; i < b_length; i++) //initialize first col
            col2[i] = i * sm.gapcost;

        for (int j = 1; j < a_length; j++) //each col
        {
            col1 = col2.clone();
            for (int i = 0; i < b_length; i++) //each row
            {
                if (i == 0)
                    col2[0] = j * sm.gapcost;
                else {
                    int gapabove = col2[i - 1] + sm.gapcost;
                    int gapleft = col1[i] + sm.gapcost;
                    int match = col1[i - 1] + sm.matrix[a[j - 1]][b[i - 1]];


                    col2[i] = max(gapabove, gapleft, match);
                }
            }
        }
        return col2[b_length - 1];
    }

    private static int quicklocalAlign(byte[] a, byte[] b, SubstitutionMatrix sm) {
        int a_length = a.length + 1; //add one position, because first row/column is gap
        int b_length = b.length + 1;
        int[] col1 = new int[b_length];
        int[] col2 = new int[b_length];

        int max_score = 0;

        for (int i = 0; i < b_length; i++) //initialize first col
            col2[i] = 0;

        for (int j = 1; j < a_length; j++) //each col
        {
            col1 = col2.clone();
            for (int i = 0; i < b_length; i++) //each row
            {
                if (i == 0)
                    col2[0] = 0;
                else {
                    int gapabove = col2[i - 1] + sm.gapcost;
                    int gapleft = col1[i] + sm.gapcost;
                    //System.out.println(i);
                    //System.out.println(j);
                    int match = col1[i - 1] + sm.matrix[a[j - 1]][b[i - 1]];
                    col2[i] = max(gapabove, gapleft, match, 0);
                    max_score = max(max_score, col2[i]);
                }
            }
        }
        return max_score;
    }

    private static int quickcfeAlign(byte[] a, byte[] b, SubstitutionMatrix sm) {
        int a_length = a.length + 1; //add one position, because first row/column is gap
        int b_length = b.length + 1;
        int[] col1 = new int[b_length];
        int[] col2 = new int[b_length];

        int max_score = 0;

        for (int i = 0; i < b_length; i++) //initialize first col
            col2[i] = 0;

        for (int j = 1; j < a_length; j++) //each col
        {
            col1 = col2.clone();
            for (int i = 0; i < b_length; i++) //each row
            {
                if (i == 0)
                    col2[0] = 0;
                else {
                    int gapabove = col2[i - 1] + sm.gapcost;
                    int gapleft = col1[i] + sm.gapcost;
                    int match = col1[i - 1] + sm.matrix[a[j - 1]][b[i - 1]];
                    col2[i] = max(gapabove, gapleft, match);

                    if (i == b_length - 1) //max score is either last column or last row
                        max_score = max(max_score, col2[b_length - 1]);
                }
            }
        }

        for (int i = 0; i < b_length; i++) {
            max_score = max(max_score, col2[i]);
        }
        return max_score;
    }

    private static int max(int a, int b) {
        return (a > b) ? a : b;
    }

    private static int max(int a, int b, int c) {
        return (a > max(b, c)) ? a : max(b, c);
    }

    private static int max(int a, int b, int c, int d) {
        return (a > max(b, c, d)) ? a : max(b, c, d);
    }

}
