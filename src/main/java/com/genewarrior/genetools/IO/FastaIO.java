/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.genewarrior.genetools.IO;

import com.genewarrior.genetools.NGS.QualitySequenceDNA;
import com.genewarrior.genetools.sequenceHandling.*;

import java.io.*;
import java.util.ArrayList;

/**
 * @author Rainer
 */
public class FastaIO {
    /**
     * Reads a DNA-Fasta-File and converts it into an ArrayList of NamedSequenceDNA
     *
     * @param f
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static ArrayList<NamedSequenceDNA> readFastaDNA(File f) throws FileNotFoundException, IOException {
        Nucleotide nuc = new Nucleotide();

        ArrayList<NamedSequenceDNA> list = new ArrayList<NamedSequenceDNA>();

        FileReader leser = new FileReader(f);
        BufferedReader reader = new BufferedReader(leser);

        String output;
        String name = "unnamed";
        //SequenceDNA sequence =new SequenceDNA("",false);
        //int line=0;
        ArrayList<String> seq = new ArrayList<String>();
        while ((output = reader.readLine()) != null) {
            //line++;
            //if (line%1000==0)
            //System.out.println("Read line "+(++line));
            if (output.startsWith(">")) {
                if (seq.size() > 0) {
                    list.add(new NamedSequenceDNA(name, new SequenceDNA(nuc.string2bytearray(seq), false)));

                    seq = new ArrayList<String>();
                    //sequence =new SequenceDNA("",false);
                }
                name = output.substring(1);
                if (name.trim().isEmpty())
                    name = "unnamed";
            } else if (!output.trim().isEmpty())
                seq.add(output.trim());
        }
        if (seq.size() > 0) {
            list.add(new NamedSequenceDNA(name, new SequenceDNA(nuc.string2bytearray(seq), false)));
        }
        reader.close();
        leser.close();

        return list;
    }

    /**
     * Reads a DNA-Fasta-File and its corresponig qual-File and converts it into an ArrayList of QualitySequence
     *
     * @param fasta   Fasta file
     * @param quality qual file
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static ArrayList<QualitySequenceDNA> readFastaQuality(File fasta, File quality) throws FileNotFoundException, IOException {

        ArrayList<QualitySequenceDNA> list = new ArrayList<QualitySequenceDNA>();

        FileReader leser1 = new FileReader(fasta);
        FileReader leser2 = new FileReader(quality);
        BufferedReader reader1 = new BufferedReader(leser1);
        BufferedReader reader2 = new BufferedReader(leser2);


        String output;
        String name = "unnamed";
        SequenceDNA sequence = new SequenceDNA("", false);
        //int line=0;
        while ((output = reader1.readLine()) != null) {
            //System.out.println("Read line "+(++line));
            if (output.startsWith(">")) {
                if (sequence.getLength() > 0) {
                    list.add(new QualitySequenceDNA(name, sequence, null));

                    sequence = new SequenceDNA("", false);
                }
                name = output.substring(1);
                if (name.trim().isEmpty())
                    name = "unnamed";
            } else if (!output.trim().isEmpty())
                sequence = SequenceTools.combineSequences(sequence, new SequenceDNA(output.trim(), false));
        }
        if (sequence.getLength() > 0) {
            list.add(new QualitySequenceDNA(name, sequence, null));
        }

        reader1.close();
        leser1.close();

        //Read the quality file
        int count = 0;
        output = "";
        name = "unnamed";
        String qualities = "";
        while ((output = reader2.readLine()) != null) {
            //System.out.println("Read line "+(++line));
            if (output.startsWith(">")) {
                if (!qualities.isEmpty()) {
                    //TODO: Check if Name in Qual files matches to the name saved from FASTA
                    String[] buffer = qualities.trim().split("\\s");
                    byte[] qual = new byte[buffer.length];
                    for (int i = 0; i < buffer.length; i++)
                        qual[i] = Byte.decode(buffer[i]);
                    list.get(count).setQuality(qual);
                    count++;
                    qualities = "";
                }
                name = output.substring(1);
                if (name.trim().isEmpty())
                    name = "unnamed";
            } else if (!output.trim().isEmpty())
                qualities = qualities + output.trim() + " ";
        }
        if (!qualities.isEmpty()) {
            //TODO: Check if Name in Qual files matches to the name saved from FASTA
            String[] buffer = qualities.trim().split("\\s");
            byte[] qual = new byte[buffer.length];
            for (int i = 0; i < buffer.length; i++)
                qual[i] = Byte.decode(buffer[i]);
            list.get(count).setQuality(qual);
        }

        reader2.close();
        leser2.close();

        return list;
    }

    /**
     * Writes the NamedSequenceDNA as a FASTA format
     *
     * @param f
     * @param NamedSequenceList
     */
    public static void writeFastaDNA(File f, ArrayList<NamedSequenceDNA> NamedSequenceList) throws IOException {
        FileWriter fstream = new FileWriter(f);
        BufferedWriter out = new BufferedWriter(fstream);

        for (NamedSequenceDNA ns : NamedSequenceList) {
            out.write(">" + ns.getName());
            out.newLine();
            out.write(ns.getSequence().toString());
            out.newLine();
        }

        out.close();
        fstream.close();
    }

    /**
     * Writes the QualitySequence as a FASTA and qual file
     *
     * @param fasta
     * @param QualitySequences
     */
    public static void writeQualitySequence(File fasta, File qual, ArrayList<QualitySequenceDNA> QualitySequences) throws IOException {
        FileWriter fstream = new FileWriter(fasta);
        FileWriter qstream = new FileWriter(qual);
        BufferedWriter outF = new BufferedWriter(fstream);
        BufferedWriter outQ = new BufferedWriter(qstream);

        for (QualitySequenceDNA ns : QualitySequences) {
            outF.write(">" + ns.getName());
            outQ.write(">" + ns.getName());
            outF.newLine();
            outQ.newLine();
            outF.write(ns.getSequence().toString());
            outF.newLine();

            for (int i = 0; i < ns.getQuality().length; i++)
                outQ.write(ns.getQuality()[i] + " ");
            outQ.newLine();
        }

        outF.close();
        fstream.close();

        outQ.close();
        qstream.close();
    }

    /**
     * Reads Protein-Fasta-File and converts it into an ArrayList of NamedSequenceAA
     *
     * @param f
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static ArrayList<NamedSequenceAA> readFastaAA(File f) throws FileNotFoundException, IOException {

        ArrayList<NamedSequenceAA> list = new ArrayList<NamedSequenceAA>();

        FileReader leser = new FileReader(f);
        BufferedReader reader = new BufferedReader(leser);

        String output;
        String name = "unnamed";
        String sequence = "";
        while ((output = reader.readLine()) != null) {
            if (output.startsWith(">")) {
                if (!sequence.isEmpty()) {
                    SequenceAA seq = new SequenceAA(sequence);
                    list.add(new NamedSequenceAA(name, seq));

                    sequence = "";
                }
                name = output.substring(1);
                if (name.trim().isEmpty())
                    name = "unnamed";
            } else if (!output.trim().isEmpty())
                sequence += output.trim();
        }
        if (!sequence.isEmpty()) {
            SequenceAA seq = new SequenceAA(sequence);
            list.add(new NamedSequenceAA(name, seq));
        }

        reader.close();
        leser.close();

        return list;
    }

    /**
     * Writes the NamedSequenceAA as a FASTA format
     *
     * @param f
     * @param NamedSequenceList
     */
    public static void writeFastaAA(File f, ArrayList<NamedSequenceAA> NamedSequenceList) throws IOException {
        FileWriter fstream = new FileWriter(f);
        BufferedWriter out = new BufferedWriter(fstream);

        for (NamedSequenceAA ns : NamedSequenceList) {
            out.write(">" + ns.getName());
            out.newLine();
            out.write(ns.getSequence().toString());
            out.newLine();
        }

        out.close();
        fstream.close();

    }
}
