package com.genewarrior.genetools.sequenceHandling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class should only be accessed by SequenceDNA and SequenceTools. Not directly.
 *
 */
public class Nucleotide {

    public final static byte _A = 1;
    public final static byte _C = 2;
    public final static byte _G = 3;
    public final static byte _T = 4;
    public final static byte _U = 5;

    public final static byte _R = 6;//G or A
    public final static byte _Y = 7;//T or C
    public final static byte _K = 8;//G or T
    public final static byte _M = 9;//A or C
    public final static byte _S = 10;//G or C
    public final static byte _W = 11;//A or T
    public final static byte _B = 12;//G or T or C
    public final static byte _D = 13;//G or A or T
    public final static byte _H = 14;//A or C or T
    public final static byte _V = 15;//G or C or A
    public final static byte _N = 16;//A or G or C or T
    public final static byte __ = 0;//Gap

    public final static int alphabetSize = 17;

    /**
     * nucleotideString looks up chars and gives byte value
     */
    public final Map<Character, Byte> nucleotideChar = new HashMap<Character, Byte>();
    /**
     * nucleotideByte looks up Byte and gives char value
     */

    public final Map<Byte, Character> nucleotideByte = new HashMap<Byte, Character>();
    /**
     * complementByte looks up Byte and gives complement byte
     */
    public final Map<Byte, Byte> complementByte = new HashMap<Byte, Byte>();

    /**
     * ambigousCode gives matching ambigous code of two nucleotides
     */
    public final byte[][] ambigousCode = new byte[alphabetSize][alphabetSize];

    public Nucleotide() {
        complementByte.put(_A, _T); //will be replaced in complement(), because it is dependent whether RNA or DNA
        complementByte.put(_C, _G);
        complementByte.put(_G, _C);
        complementByte.put(_T, _A);
        complementByte.put(_U, _A);
        complementByte.put(_R, _Y);
        complementByte.put(_Y, _R);
        complementByte.put(_K, _M);
        complementByte.put(_M, _K);
        complementByte.put(_S, _S);
        complementByte.put(_W, _W);
        complementByte.put(_B, _V);
        complementByte.put(_D, _H);
        complementByte.put(_H, _D);
        complementByte.put(_V, _B);
        complementByte.put(_N, _N);
        complementByte.put(__, __);

        ambigousCode[_A][_A] = _A;
        ambigousCode[_A][_C] = ambigousCode[_C][_A] = _M;
        ambigousCode[_A][_G] = ambigousCode[_G][_A] = _R;
        ambigousCode[_A][_T] = ambigousCode[_T][_A] = _W;
        ambigousCode[_A][_R] = ambigousCode[_R][_A] = _R;
        ambigousCode[_A][_Y] = ambigousCode[_Y][_A] = _H;
        ambigousCode[_A][_S] = ambigousCode[_S][_A] = _V;
        ambigousCode[_A][_W] = ambigousCode[_W][_A] = _W;
        ambigousCode[_A][_K] = ambigousCode[_K][_A] = _D;
        ambigousCode[_A][_M] = ambigousCode[_M][_A] = _M;
        ambigousCode[_A][_B] = ambigousCode[_B][_A] = _N;
        ambigousCode[_A][_D] = ambigousCode[_D][_A] = _D;
        ambigousCode[_A][_H] = ambigousCode[_H][_A] = _H;
        ambigousCode[_A][_V] = ambigousCode[_V][_A] = _V;
        ambigousCode[_A][_N] = ambigousCode[_N][_A] = _N;
        ambigousCode[_A][__] = ambigousCode[__][_A] = _A;
        ambigousCode[_C][_C] = _C;
        ambigousCode[_C][_G] = ambigousCode[_G][_C] = _S;
        ambigousCode[_C][_T] = ambigousCode[_T][_C] = _Y;
        ambigousCode[_C][_R] = ambigousCode[_R][_C] = _V;
        ambigousCode[_C][_Y] = ambigousCode[_Y][_C] = _Y;
        ambigousCode[_C][_S] = ambigousCode[_S][_C] = _S;
        ambigousCode[_C][_W] = ambigousCode[_W][_C] = _H;
        ambigousCode[_C][_K] = ambigousCode[_K][_C] = _B;
        ambigousCode[_C][_M] = ambigousCode[_M][_C] = _M;
        ambigousCode[_C][_B] = ambigousCode[_B][_C] = _B;
        ambigousCode[_C][_D] = ambigousCode[_D][_C] = _N;
        ambigousCode[_C][_H] = ambigousCode[_H][_C] = _H;
        ambigousCode[_C][_V] = ambigousCode[_V][_C] = _V;
        ambigousCode[_C][_N] = ambigousCode[_N][_C] = _N;
        ambigousCode[_C][__] = ambigousCode[__][_C] = _C;
        ambigousCode[_G][_G] = _G;
        ambigousCode[_G][_T] = ambigousCode[_T][_G] = _K;
        ambigousCode[_G][_R] = ambigousCode[_R][_G] = _R;
        ambigousCode[_G][_Y] = ambigousCode[_Y][_G] = _B;
        ambigousCode[_G][_S] = ambigousCode[_S][_G] = _S;
        ambigousCode[_G][_W] = ambigousCode[_W][_G] = _D;
        ambigousCode[_G][_K] = ambigousCode[_K][_G] = _K;
        ambigousCode[_G][_M] = ambigousCode[_M][_G] = _V;
        ambigousCode[_G][_B] = ambigousCode[_B][_G] = _B;
        ambigousCode[_G][_D] = ambigousCode[_D][_G] = _D;
        ambigousCode[_G][_H] = ambigousCode[_H][_G] = _N;
        ambigousCode[_G][_V] = ambigousCode[_V][_G] = _V;
        ambigousCode[_G][_N] = ambigousCode[_N][_G] = _N;
        ambigousCode[_G][__] = ambigousCode[__][_G] = _G;
        ambigousCode[_T][_T] = _T;
        ambigousCode[_T][_R] = ambigousCode[_R][_T] = _D;
        ambigousCode[_T][_Y] = ambigousCode[_Y][_T] = _Y;
        ambigousCode[_T][_S] = ambigousCode[_S][_T] = _B;
        ambigousCode[_T][_W] = ambigousCode[_W][_T] = _W;
        ambigousCode[_T][_K] = ambigousCode[_K][_T] = _K;
        ambigousCode[_T][_M] = ambigousCode[_M][_T] = _H;
        ambigousCode[_T][_B] = ambigousCode[_B][_T] = _B;
        ambigousCode[_T][_D] = ambigousCode[_D][_T] = _D;
        ambigousCode[_T][_H] = ambigousCode[_H][_T] = _H;
        ambigousCode[_T][_V] = ambigousCode[_V][_T] = _N;
        ambigousCode[_T][_N] = ambigousCode[_N][_T] = _N;
        ambigousCode[_T][__] = ambigousCode[__][_T] = _T;
        ambigousCode[_R][_R] = _R;
        ambigousCode[_R][_Y] = ambigousCode[_Y][_R] = _N;
        ambigousCode[_R][_S] = ambigousCode[_S][_R] = _V;
        ambigousCode[_R][_W] = ambigousCode[_W][_R] = _D;
        ambigousCode[_R][_K] = ambigousCode[_K][_R] = _D;
        ambigousCode[_R][_M] = ambigousCode[_M][_R] = _V;
        ambigousCode[_R][_B] = ambigousCode[_B][_R] = _N;
        ambigousCode[_R][_D] = ambigousCode[_D][_R] = _D;
        ambigousCode[_R][_H] = ambigousCode[_H][_R] = _N;
        ambigousCode[_R][_V] = ambigousCode[_V][_R] = _V;
        ambigousCode[_R][_N] = ambigousCode[_N][_R] = _N;
        ambigousCode[_R][__] = ambigousCode[__][_R] = _R;
        ambigousCode[_Y][_Y] = _Y;
        ambigousCode[_Y][_S] = ambigousCode[_S][_Y] = _B;
        ambigousCode[_Y][_W] = ambigousCode[_W][_Y] = _H;
        ambigousCode[_Y][_K] = ambigousCode[_K][_Y] = _B;
        ambigousCode[_Y][_M] = ambigousCode[_M][_Y] = _H;
        ambigousCode[_Y][_B] = ambigousCode[_B][_Y] = _B;
        ambigousCode[_Y][_D] = ambigousCode[_D][_Y] = _N;
        ambigousCode[_Y][_H] = ambigousCode[_H][_Y] = _H;
        ambigousCode[_Y][_V] = ambigousCode[_V][_Y] = _N;
        ambigousCode[_Y][_N] = ambigousCode[_N][_Y] = _N;
        ambigousCode[_Y][__] = ambigousCode[__][_Y] = _Y;
        ambigousCode[_S][_S] = _S;
        ambigousCode[_S][_W] = ambigousCode[_W][_S] = _N;
        ambigousCode[_S][_K] = ambigousCode[_K][_S] = _B;
        ambigousCode[_S][_M] = ambigousCode[_M][_S] = _V;
        ambigousCode[_S][_B] = ambigousCode[_B][_S] = _B;
        ambigousCode[_S][_D] = ambigousCode[_D][_S] = _N;
        ambigousCode[_S][_H] = ambigousCode[_H][_S] = _N;
        ambigousCode[_S][_V] = ambigousCode[_V][_S] = _V;
        ambigousCode[_S][_N] = ambigousCode[_N][_S] = _N;
        ambigousCode[_S][__] = ambigousCode[__][_S] = _S;
        ambigousCode[_W][_W] = _W;
        ambigousCode[_W][_K] = ambigousCode[_K][_W] = _D;
        ambigousCode[_W][_M] = ambigousCode[_M][_W] = _H;
        ambigousCode[_W][_B] = ambigousCode[_B][_W] = _N;
        ambigousCode[_W][_D] = ambigousCode[_D][_W] = _D;
        ambigousCode[_W][_H] = ambigousCode[_H][_W] = _H;
        ambigousCode[_W][_V] = ambigousCode[_V][_W] = _N;
        ambigousCode[_W][_N] = ambigousCode[_N][_W] = _N;
        ambigousCode[_W][__] = ambigousCode[__][_W] = _W;
        ambigousCode[_K][_K] = _K;
        ambigousCode[_K][_M] = ambigousCode[_M][_K] = _N;
        ambigousCode[_K][_B] = ambigousCode[_B][_K] = _B;
        ambigousCode[_K][_D] = ambigousCode[_D][_K] = _D;
        ambigousCode[_K][_H] = ambigousCode[_H][_K] = _N;
        ambigousCode[_K][_V] = ambigousCode[_V][_K] = _N;
        ambigousCode[_K][_N] = ambigousCode[_N][_K] = _N;
        ambigousCode[_K][__] = ambigousCode[__][_K] = _K;
        ambigousCode[_M][_M] = _M;
        ambigousCode[_M][_B] = ambigousCode[_B][_M] = _N;
        ambigousCode[_M][_D] = ambigousCode[_D][_M] = _N;
        ambigousCode[_M][_H] = ambigousCode[_H][_M] = _H;
        ambigousCode[_M][_V] = ambigousCode[_V][_M] = _V;
        ambigousCode[_M][_N] = ambigousCode[_N][_M] = _N;
        ambigousCode[_M][__] = ambigousCode[__][_M] = _M;
        ambigousCode[_B][_B] = _B;
        ambigousCode[_B][_D] = ambigousCode[_D][_B] = _N;
        ambigousCode[_B][_H] = ambigousCode[_H][_B] = _N;
        ambigousCode[_B][_V] = ambigousCode[_V][_B] = _N;
        ambigousCode[_B][_N] = ambigousCode[_N][_B] = _N;
        ambigousCode[_B][__] = ambigousCode[__][_B] = _B;
        ambigousCode[_D][_D] = _D;
        ambigousCode[_D][_H] = ambigousCode[_H][_D] = _N;
        ambigousCode[_D][_V] = ambigousCode[_V][_D] = _N;
        ambigousCode[_D][_N] = ambigousCode[_N][_D] = _N;
        ambigousCode[_D][__] = ambigousCode[__][_D] = _D;
        ambigousCode[_H][_H] = _H;
        ambigousCode[_H][_V] = ambigousCode[_V][_H] = _N;
        ambigousCode[_H][_N] = ambigousCode[_N][_H] = _N;
        ambigousCode[_H][__] = ambigousCode[__][_H] = _H;
        ambigousCode[_V][_V] = _V;
        ambigousCode[_V][_N] = ambigousCode[_N][_V] = _N;
        ambigousCode[_V][__] = ambigousCode[__][_V] = _V;
        ambigousCode[_N][_N] = _N;
        ambigousCode[_N][__] = ambigousCode[__][_N] = _N;
        ambigousCode[__][__] = ambigousCode[__][__] = __;

        nucleotideChar.put('A', _A);
        nucleotideChar.put('C', _C);
        nucleotideChar.put('G', _G);
        nucleotideChar.put('T', _T);
        nucleotideChar.put('U', _U);
        nucleotideChar.put('R', _R);
        nucleotideChar.put('Y', _Y);
        nucleotideChar.put('K', _K);
        nucleotideChar.put('M', _M);
        nucleotideChar.put('S', _S);
        nucleotideChar.put('W', _W);
        nucleotideChar.put('B', _B);
        nucleotideChar.put('D', _D);
        nucleotideChar.put('H', _H);
        nucleotideChar.put('V', _V);
        nucleotideChar.put('N', _N);
        nucleotideChar.put(' ', __);
        nucleotideChar.put('-', __);

        nucleotideByte.put(_A, 'A');
        nucleotideByte.put(_C, 'C');
        nucleotideByte.put(_G, 'G');
        nucleotideByte.put(_T, 'T');
        nucleotideByte.put(_U, 'U');
        nucleotideByte.put(_R, 'R');
        nucleotideByte.put(_Y, 'Y');
        nucleotideByte.put(_K, 'K');
        nucleotideByte.put(_M, 'M');
        nucleotideByte.put(_S, 'S');
        nucleotideByte.put(_W, 'W');
        nucleotideByte.put(_B, 'B');
        nucleotideByte.put(_D, 'D');
        nucleotideByte.put(_H, 'H');
        nucleotideByte.put(_V, 'V');
        nucleotideByte.put(_N, 'N');
        nucleotideByte.put(__, '-');
    }

    /**
     * compares nucleotides. Ambigous nucleotide <code>A</code> is expanded and compared to <code>B</code>. E.g.: A='N',B='C' is true, but vice versa is false. Can be used to compare RNA to DNA (U==T).
     *
     * @param A
     * @param B
     * @return
     */
    public static boolean compareNucleotide(byte A, byte B) {
        if (A == _U)
            A = _T;
        if (B == _U)
            B = _T;

        if (A == B)
            return true;

        if (A == _R && (B == _G || B == _A))
            return true;

        if (A == _Y && (B == _T || B == _C))
            return true;

        if (A == _K && (B == _G || B == _T))
            return true;

        if (A == _M && (B == _A || B == _C))
            return true;

        if (A == _S && (B == _G || B == _C))
            return true;

        if (A == _W && (B == _A || B == _T))
            return true;

        if (A == _B && (B == _G || B == _T || B == _C || B == _K || B == _S || B == _Y))
            return true;

        if (A == _D && (B == _G || B == _A || B == _T || B == _R || B == _K || B == _W))
            return true;

        if (A == _H && (B == _A || B == _T || B == _C || B == _W || B == _M || B == _Y))
            return true;

        if (A == _V && (B == _G || B == _A || B == _C || B == _R || B == _S || B == _M))
            return true;

        return A == _N;
    }

    /**
     * Returns Invalid Positions of String. USE <code>SequenceTools.invalidPositionsDNA(String)</code> TO DO THIS TASK!
     *
     * @param seq
     * @return
     */
    public int[] returnInvalidPositions(String seq) {
        char[] seqArray = seq.toUpperCase().toCharArray();
        ArrayList<Integer> invalids = new ArrayList<Integer>();

        for (int i = 0; i < seqArray.length; i++) {
            if (nucleotideChar.get(seqArray[i]) == null)
                invalids.add(i);
        }

        int[] invalidIndex = new int[invalids.size()];


        for (int i = 0; i < invalidIndex.length; i++) {
            invalidIndex[i] = invalids.get(i);
        }

        return invalidIndex;
    }

    /**
     * Converts String sequence to Byte Array.
     *
     * @param seq Sequence to convert to Byte[]
     * @return byte for each nucleotide or gap if no valid nucleotide
     */
    public byte[] string2bytearray(String seq) {
        byte[] nucleotides = new byte[seq.length()];
        char[] seqArray = seq.toUpperCase().toCharArray();
        for (int i = 0; i < seqArray.length; i++) {
            if (nucleotideChar.get(seqArray[i]) == null)
                nucleotides[i] = __;
            else
                nucleotides[i] = nucleotideChar.get(seqArray[i]);
        }
        return nucleotides;
    }

    /**
     * Converts ArrayList of String sequence to Byte Array (concatenation of the array to one sequence; helper function for reading fasta files).
     *
     * @param seq ArrayList of Sequence to convert to Byte[], all ordered
     * @return byte for each nucleotide or gap if no valid nucleotide
     */
    public byte[] string2bytearray(ArrayList<String> seq) {
        int length = 0;
        for (String s : seq)
            length += s.length();

        byte[] nucleotides = new byte[length];

        int pos = 0;
        for (String s : seq) {
            char[] seqArray = s.toUpperCase().toCharArray();
            for (int i = 0; i < seqArray.length; i++) {
                if (nucleotideChar.get(seqArray[i]) == null)
                    nucleotides[pos++] = __;
                else
                    nucleotides[pos++] = nucleotideChar.get(seqArray[i]);
            }
        }


        return nucleotides;
    }

    /**
     * Converts Byte Array to String sequence
     *
     * @param seq
     * @return
     */
    public String bytearray2string(byte[] seq) {
        char[] nucleotides = new char[seq.length];

        for (int i = 0; i < nucleotides.length; i++) {
            if (nucleotideByte.get(seq[i]) == null)
                nucleotides[i] = '-';
            else
                nucleotides[i] = nucleotideByte.get(seq[i]);
        }

        return new String(nucleotides);
    }

    /**
     * Delete Gaps in byte-array
     *
     * @param seq
     * @return
     */
    public byte[] killGaps(byte[] seq) {
        //count gaps
        int gaps = 0;
        for (byte d : seq) {
            if (d == __)
                gaps++;
        }

        if (gaps == 0)
            return seq;

        //delete gaps
        byte[] seq_nogaps = new byte[seq.length - gaps];
        int pos = 0; //count position

        for (int i = 0; i < seq.length; i++) {
            if (seq[i] != __) {
                seq_nogaps[pos] = seq[i];
                pos++;
            }
        }

        return seq_nogaps;

    }

    /**
     * Returns the ambigous nucleotide, e.g. ambigous(Nucleotide._A, Nucleotdide._T) returns Nucleotide._W
     *
     * @param a
     * @param b
     * @return
     */
    public byte ambigous(byte a, byte b) {
        return ambigousCode[(a == _U ? _T : a)][(b == _U ? _T : b)];
    }

    /**
     * Returns the ambigous nucleotide, e.g. ambigous('A', 'T') returns 'W'
     *
     * @param a
     * @param b
     * @return
     */
    public char ambigous(char a, char b) {
        byte a_ = nucleotideChar.get(a);
        byte b_ = nucleotideChar.get(b);
        return nucleotideByte.get(ambigous(a_, b_));
    }

    /**
     * Returns complement of byteArray
     *
     * @param seq
     * @param isRNA if true complement of A is U, if false complement of A is T
     * @return
     */
    public byte[] complement(byte[] seq, boolean isRNA) {
        byte[] revnucleotides = new byte[seq.length];

        for (int i = 0; i < seq.length; i++) {
            if (complementByte.get(seq[i]) == null)
                revnucleotides[i] = __;
            else
                revnucleotides[i] = complementByte.get(seq[i]);
        }

        return revnucleotides;
    }

    /**
     * Changes Byte in given Byte-Array to another Byte. (Change all nucleotides in sequence).
     *
     * @param seq
     * @param fromByte
     * @param toByte
     * @return
     */
    public byte[] changeByte(byte[] seq, byte fromByte, byte toByte) {
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] == fromByte)
                seq[i] = toByte;
        }

        return seq;
    }

    /**
     * Counts nucleotides in given Byte-Array.
     *
     * @param seq
     * @return int[17]: counts of nucleotides; Index: Nucleotide-Value (Byte-Value); Value: Occurence of Nucleotide in given Sequence.
     */
    public int[] countNucleots(byte[] seq) {
        int[] count = new int[alphabetSize];

        for (int i = 0; i < seq.length; i++) {
            count[seq[i]]++;
        }
        return count;
    }


}
