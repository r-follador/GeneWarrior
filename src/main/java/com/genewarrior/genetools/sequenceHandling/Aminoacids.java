/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.sequenceHandling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author kingcarlxx
 */
public class Aminoacids {
    public final static byte _A = 1;
    public final static byte _R = 2;
    public final static byte _N = 3;
    public final static byte _D = 4;
    public final static byte _C = 5;
    public final static byte _Q = 6;
    public final static byte _E = 7;
    public final static byte _G = 8;
    public final static byte _H = 9;
    public final static byte _I = 10;
    public final static byte _L = 11;
    public final static byte _K = 12;
    public final static byte _M = 13;
    public final static byte _F = 14;
    public final static byte _P = 15;
    public final static byte _O = 16;
    public final static byte _S = 17;
    public final static byte _U = 18;
    public final static byte _T = 19;
    public final static byte _W = 20;
    public final static byte _Y = 21;
    public final static byte _V = 22;
    public final static byte _B = 23;
    public final static byte _Z = 24;
    public final static byte _X = 25;
    public final static byte _J = 26;
    public final static byte _TERM = 27;
    public final static byte __ = 0;//Gap

    public final static int alphabetSize = 28;

    Nucleotide nucl = new Nucleotide();

    /**
     * OneLetterChar looks up bytes and gives one letter aa codes
     */
    public final char[] OneLetterChar = new char[alphabetSize];
    /**
     * ThreeLetterString looks up bytes and gives three letter aa codes
     */
    public final String[] ThreeLetterString = new String[alphabetSize];
    /**
     * aaChar looks up chars (one letter code) and gives byte
     */
    public final Map<Character, Byte> aaChar = new HashMap<Character, Byte>();
    /**
     * aa3Char looks up string (three letter code) and gives byte
     */
    public final Map<String, Byte> aa3String = new HashMap<String, Byte>();

    /**
     * TranslationTables
     * [Code Nr.][0=normal/1=codonstart][firstBp][secondBp][thirdBp]
     */
    public final byte[][][][][] translationTable = new byte[25][2][Nucleotide.alphabetSize][Nucleotide.alphabetSize][Nucleotide.alphabetSize];

    /**
     *
     */

    public final String[] translationTableDescription = new String[25];

    public Aminoacids() {
        OneLetterChar[_A] = 'A';
        OneLetterChar[_R] = 'R';
        OneLetterChar[_N] = 'N';
        OneLetterChar[_D] = 'D';
        OneLetterChar[_C] = 'C';
        OneLetterChar[_Q] = 'Q';
        OneLetterChar[_E] = 'E';
        OneLetterChar[_G] = 'G';
        OneLetterChar[_H] = 'H';
        OneLetterChar[_I] = 'I';
        OneLetterChar[_L] = 'L';
        OneLetterChar[_K] = 'K';
        OneLetterChar[_M] = 'M';
        OneLetterChar[_F] = 'F';
        OneLetterChar[_P] = 'P';
        OneLetterChar[_O] = 'O';
        OneLetterChar[_S] = 'S';
        OneLetterChar[_U] = 'U';
        OneLetterChar[_T] = 'T';
        OneLetterChar[_W] = 'W';
        OneLetterChar[_Y] = 'Y';
        OneLetterChar[_V] = 'V';
        OneLetterChar[_B] = 'B';
        OneLetterChar[_Z] = 'Z';
        OneLetterChar[_X] = 'X';
        OneLetterChar[_J] = 'J';
        OneLetterChar[_TERM] = '*'; //termination
        OneLetterChar[__] = '-';

        ThreeLetterString[_A] = "Ala";
        ThreeLetterString[_R] = "Arg";
        ThreeLetterString[_N] = "Asn";
        ThreeLetterString[_D] = "Asp";
        ThreeLetterString[_C] = "Cys";
        ThreeLetterString[_Q] = "Gln";
        ThreeLetterString[_E] = "Glu";
        ThreeLetterString[_G] = "Gly";
        ThreeLetterString[_H] = "His";
        ThreeLetterString[_I] = "Ile";
        ThreeLetterString[_L] = "Leu";
        ThreeLetterString[_K] = "Lys";
        ThreeLetterString[_M] = "Met";
        ThreeLetterString[_F] = "Phe";
        ThreeLetterString[_P] = "Pro";
        ThreeLetterString[_O] = "Pyl";
        ThreeLetterString[_S] = "Ser";
        ThreeLetterString[_U] = "Sec";
        ThreeLetterString[_T] = "Thr";
        ThreeLetterString[_W] = "Trp";
        ThreeLetterString[_Y] = "Tyr";
        ThreeLetterString[_V] = "Val";
        ThreeLetterString[_B] = "Asx";
        ThreeLetterString[_Z] = "Glx";
        ThreeLetterString[_X] = "Xaa";
        ThreeLetterString[_J] = "Xle";
        ThreeLetterString[_TERM] = "TER";
        ThreeLetterString[__] = "---";

        aaChar.put(OneLetterChar[_A], _A);
        aaChar.put(OneLetterChar[_R], _R);
        aaChar.put(OneLetterChar[_N], _N);
        aaChar.put(OneLetterChar[_D], _D);
        aaChar.put(OneLetterChar[_C], _C);
        aaChar.put(OneLetterChar[_Q], _Q);
        aaChar.put(OneLetterChar[_E], _E);
        aaChar.put(OneLetterChar[_G], _G);
        aaChar.put(OneLetterChar[_H], _H);
        aaChar.put(OneLetterChar[_I], _I);
        aaChar.put(OneLetterChar[_L], _L);
        aaChar.put(OneLetterChar[_K], _K);
        aaChar.put(OneLetterChar[_M], _M);
        aaChar.put(OneLetterChar[_F], _F);
        aaChar.put(OneLetterChar[_P], _P);
        aaChar.put(OneLetterChar[_O], _O);
        aaChar.put(OneLetterChar[_S], _S);
        aaChar.put(OneLetterChar[_U], _U);
        aaChar.put(OneLetterChar[_T], _T);
        aaChar.put(OneLetterChar[_W], _W);
        aaChar.put(OneLetterChar[_Y], _Y);
        aaChar.put(OneLetterChar[_V], _V);
        aaChar.put(OneLetterChar[_B], _B);
        aaChar.put(OneLetterChar[_Z], _Z);
        aaChar.put(OneLetterChar[_X], _X);
        aaChar.put(OneLetterChar[_J], _J);
        aaChar.put(OneLetterChar[_TERM], _TERM);
        aaChar.put(OneLetterChar[__], __);

        aa3String.put(ThreeLetterString[_A].toLowerCase(), _A);
        aa3String.put(ThreeLetterString[_R].toLowerCase(), _R);
        aa3String.put(ThreeLetterString[_N].toLowerCase(), _N);
        aa3String.put(ThreeLetterString[_D].toLowerCase(), _D);
        aa3String.put(ThreeLetterString[_C].toLowerCase(), _C);
        aa3String.put(ThreeLetterString[_Q].toLowerCase(), _Q);
        aa3String.put(ThreeLetterString[_E].toLowerCase(), _E);
        aa3String.put(ThreeLetterString[_G].toLowerCase(), _G);
        aa3String.put(ThreeLetterString[_H].toLowerCase(), _H);
        aa3String.put(ThreeLetterString[_I].toLowerCase(), _I);
        aa3String.put(ThreeLetterString[_L].toLowerCase(), _L);
        aa3String.put(ThreeLetterString[_K].toLowerCase(), _K);
        aa3String.put(ThreeLetterString[_M].toLowerCase(), _M);
        aa3String.put(ThreeLetterString[_F].toLowerCase(), _F);
        aa3String.put(ThreeLetterString[_P].toLowerCase(), _P);
        aa3String.put(ThreeLetterString[_O].toLowerCase(), _O);
        aa3String.put(ThreeLetterString[_S].toLowerCase(), _S);
        aa3String.put(ThreeLetterString[_U].toLowerCase(), _U);
        aa3String.put(ThreeLetterString[_T].toLowerCase(), _T);
        aa3String.put(ThreeLetterString[_W].toLowerCase(), _W);
        aa3String.put(ThreeLetterString[_Y].toLowerCase(), _Y);
        aa3String.put(ThreeLetterString[_V].toLowerCase(), _V);
        aa3String.put(ThreeLetterString[_B].toLowerCase(), _B);
        aa3String.put(ThreeLetterString[_Z].toLowerCase(), _Z);
        aa3String.put(ThreeLetterString[_X].toLowerCase(), _X);
        aa3String.put(ThreeLetterString[_J].toLowerCase(), _J);
        aa3String.put(ThreeLetterString[_TERM].toLowerCase(), _TERM);
        aa3String.put(ThreeLetterString[__].toLowerCase(), __);

        //http://www.ncbi.nlm.nih.gov/Taxonomy/Utils/wprintgc.cgi

        setTranslationTable(1, "Standard Code", "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "---M---------------M---------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(2, "Vertebrate Mitochondrial Code", "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSS**VVVVAAAADDEEGGGG", "--------------------------------MMMM---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(3, "Yeast Mitochondrial Code", "FFLLSSSSYY**CCWWTTTTPPPPHHQQRRRRIIMMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "----------------------------------MM----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(4, "Mold, Protozoan, Coelenterate Mitochondrial Code & Mycoplasma/Spiroplasma Code", "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "--MM---------------M------------MMMM---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(5, "Invertebrate Mitochondrial Code", "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSSSVVVVAAAADDEEGGGG", "---M----------------------------MMMM---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(6, "Ciliate, Dasycladacean and Hexamita Nuclear Code", "FFLLSSSSYYQQCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "-----------------------------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(9, "Echinoderm and Flatworm Mitochondrial Code", "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG", "-----------------------------------M---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(10, "Euplotid Nuclear Code", "FFLLSSSSYY**CCCWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "-----------------------------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(11, "Bacterial and Plant Plastid Code", "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "---M---------------M------------MMMM---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(12, "Alternative Yeast Nuclear Code", "FFLLSSSSYY**CC*WLLLSPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "-------------------M---------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(13, "Ascidian Mitochondrial Code", "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSGGVVVVAAAADDEEGGGG", "---M------------------------------MM---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(14, "Alternative Flatworm Mitochondrial Code", "FFLLSSSSYYY*CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG", "-----------------------------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(15, "Blepharisma Nuclear Code", "FFLLSSSSYY*QCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "-----------------------------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(16, "Chlorophycean Mitochondrial Code", "FFLLSSSSYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "-----------------------------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(21, "Trematode Mitochondrial Code", "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNNKSSSSVVVVAAAADDEEGGGG", "-----------------------------------M---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(22, "Scenedesmus obliquus mitochondrial Code", "FFLLSS*SYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "-----------------------------------M----------------------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(23, "Thraustochytrium Mitochondrial Code", "FF*LSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", "--------------------------------M--M---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");
        setTranslationTable(24, "Pterobranchia mitochondrial Code", "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSSKVVVVAAAADDEEGGGG", "---M---------------M---------------M---------------M------------", "TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG", "TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG", "TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG");

    }

    private void setTranslationTable(int codenr, String Description, String AA, String StartAA, String FirstBase, String SecondBase, String ThirdBase) {
        byte[] aminoacids = string2bytearray(AA);
        byte[] startamino = string2bytearray(StartAA);
        byte[] firstbase = nucl.string2bytearray(FirstBase);
        byte[] secondbase = nucl.string2bytearray(SecondBase);
        byte[] thirdbase = nucl.string2bytearray(ThirdBase);

        translationTableDescription[codenr] = Description;

        for (int i = 0; i < aminoacids.length; i++) {
            translationTable[codenr][0][firstbase[i]][secondbase[i]][thirdbase[i]] = aminoacids[i];
            translationTable[codenr][1][firstbase[i]][secondbase[i]][thirdbase[i]] = startamino[i];
        }
    }


    /**
     * Returns Invalid Positions of String (Single Letter code AA). USE <code>SequenceTools.invalidPositionsProtein(String)</code> TO DO THIS TASK!
     *
     * @param seq
     * @return
     */
    public int[] returnInvalidPositions(String seq) {
        char[] seqArray = seq.toUpperCase().toCharArray();
        ArrayList<Integer> invalids = new ArrayList<Integer>();

        for (int i = 0; i < seqArray.length; i++) {
            if (aaChar.get(seqArray[i]) == null)
                invalids.add(i);
        }

        int[] invalidIndex = new int[invalids.size()];


        for (int i = 0; i < invalidIndex.length; i++) {
            invalidIndex[i] = invalids.get(i);
        }

        return invalidIndex;
    }

    /**
     * Converts String sequence (one letter aa code) to Byte Array.
     *
     * @param seq Protein-Sequence to convert to Byte[]
     * @return byte for each nucleotide or gap if no valid aa
     */
    public byte[] string2bytearray(String seq) {
        byte[] nucleotides = new byte[seq.length()];
        char[] seqArray = seq.toUpperCase().toCharArray();
        for (int i = 0; i < seqArray.length; i++) {
            if (aaChar.get(seqArray[i]) == null)
                nucleotides[i] = __;
            else
                nucleotides[i] = aaChar.get(seqArray[i]);
        }
        return nucleotides;
    }

    /**
     * Converts Byte Array to String protein-sequence (one-letter aa code)
     *
     * @param seq
     * @return
     */
    public String bytearray2string1Letter(byte[] seq) {
        char[] nucleotides = new char[seq.length];

        for (int i = 0; i < nucleotides.length; i++) {
            nucleotides[i] = OneLetterChar[seq[i]];
        }

        return new String(nucleotides);
    }

    /**
     * Converts Byte Array to String protein-sequence (three-letter aa code)
     *
     * @param seq
     * @return
     */
    public String bytearray2string3Letter(byte[] seq) {
        String nucleotides = "";

        for (int i = 0; i < seq.length; i++) {
            if (seq[i] > seq.length - 1 || seq[i] < 0)
                nucleotides += "---";
            else
                nucleotides += ThreeLetterString[seq[i]];
        }

        return nucleotides;
    }

    /**
     * Changes Byte in given Byte-Array to another Byte. (Change all aa in sequence).
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
     * Delete Gaps in byte-array
     *
     * @param seq
     * @return
     */
    public byte[] killGaps(byte[] seq) {
        //count gaps
        int gaps = 0;
        for (byte d : seq) {
            if (d == __ || d > 26)
                gaps++;
        }

        if (gaps == 0)
            return seq;

        //delete gaps
        byte[] seq_nogaps = new byte[seq.length - gaps];
        int pos = 0; //count position

        for (int i = 0; i < seq.length; i++) {
            if (seq[i] != __ && !(seq[i] > 26)) {
                seq_nogaps[pos] = seq[i];
                pos++;
            }
        }

        return seq_nogaps;
    }

    /**
     * compares Aminoacids. Ambigous nucleotide <code>A</code> is expanded and compared to <code>B</code>. E.g.: A='X',B='C' is true, but vice versa is false.
     *
     * @param A
     * @param B
     * @return
     */
    public static boolean compareAminoacids(byte A, byte B) {
        if (A == B)
            return true;
        if (A == _B && (B == _N || B == _D))
            return true;
        if (A == _Z && (B == _Q || B == _E))
            return true;
        if (A == _J && (B == _I || B == _L))
            return true;
        return A == _X && (B != __) && (B != _TERM);
    }

    /**
     * Counts aminoacids in given Byte-Array.
     *
     * @param seq
     * @return int[]: counts of nucleotides; Index: Aminoacid-Value (Byte-Value); Value: Occurence of Aminoacid in given Sequence.
     */
    public int[] countAminos(byte[] seq) {
        int[] count = new int[alphabetSize];

        for (int i = 0; i < seq.length; i++) {
            count[seq[i]]++;
        }
        return count;
    }

}
