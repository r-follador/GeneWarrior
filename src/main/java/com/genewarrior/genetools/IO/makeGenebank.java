/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.IO;

import com.genewarrior.genetools.Contig;
import com.genewarrior.genetools.Feature;
import com.genewarrior.genetools.Species;
import com.genewarrior.genetools.sequenceHandling.Position;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author btserver
 */
public class makeGenebank {

    public static boolean makeGenebank(Contig contig, File file) {
        ArrayList<String> output = new ArrayList<String>();

        output.add("LOCUS \t XXXxxxxx \t " + contig.getLength() + " bp \t DNA \t circular \t BCT " + ((new SimpleDateFormat("dd-MMM-yyyy")).format(new Date())));
        output.add("DEFINITION \t" + ((Species) contig.getParent()).name + ", " + contig.getName());
        output.add("ACCESSION \t XX_xxxxxx");
        output.add("VERSION \t XX_xxxxxx");
        output.add("SOURCE \t " + ((Species) contig.getParent()).name);
        output.add("  ORGANISM \t " + ((Species) contig.getParent()).name);
        output.add("\t XXXX; XXXX; XXXX; XXXX");
        output.add("REFERENCE \t 1  (bases 1 to " + contig.getLength() + ")");
        output.add("  AUTHORS \t XXXX, XXXX and XXXX");
        output.add("  TITLE \t XXXX");
        output.add("  JOURNAL \t XXXX");
        output.add("FEATURES \t \t Location/Qualifiers");
        output.add("\t source \t 1.." + contig.getLength());
        output.add("\t\t /organism=\"" + ((Species) contig.getParent()).name + "\"");
        output.add("\t\t /mol_type=\"genomic DNA\"");
        output.add("\t\t /strain=\"XXXX\"");
        output.add("\t\t /db_xref=\"taxon:XXX\"");

        for (int i = 0; i < contig.getLength(); i++) {
            for (Feature f : contig.featureSet) {
                if (f.position.getStartPos() == i) {
                    String position = (f.position.isReverse() ? "complement(" : "") + (f.position.getStartPos() + 1) + ".." + (f.position.getEndPos() + 1) + (f.position.isReverse() ? ")" : "");
                    if (f.isProteinProduct()) {
                        output.add("\t gene \t " + position);
                        if (!f.name[1].isEmpty())
                            output.add("\t\t /gene=\"" + f.name[1] + "\"");
                        output.add("\t\t /locus_tag=\"" + f.name[0] + "\"");
                        output.add("\t CDS \t " + position);
                        if (!f.name[1].isEmpty())
                            output.add("\t\t /gene=\"" + f.name[1] + "\"");
                        output.add("\t\t /locus_tag=\"" + f.name[0] + "\"");
                        output.add("\t\t /product=\"" + f.getProteinProduct().getProduct() + "\"");
                        output.add("\t\t /codon_start=\"1\"");
                        output.add("\t\t /transl_table=\"" + f.getProteinProduct().getTranslTable() + "\"");
                        output.add("\t\t /translation=\"" + f.getProteinProduct().getSequence().toString() + "\"");
                    } else //not a protein product
                    {
                        output.add("\t gene \t " + position);
                        if (!f.name[1].isEmpty())
                            output.add("\t\t /gene=\"" + f.name[1] + "\"");
                        output.add("\t\t /locus_tag=\"" + f.name[0] + "\"");
                        if (f.name[1].contains("rRNA")) {
                            output.add("\t rRNA \t " + position);
                        } else if (f.name[1].contains("tRNA")) {
                            output.add("\t tRNA \t " + position);
                        } else {
                            output.add("----ERROR---" + position);
                        }
                        if (!f.name[1].isEmpty())
                            output.add("\t\t /gene=\"" + f.name[1] + "\"");
                        output.add("\t\t /locus_tag=\"" + f.name[0] + "\"");
                    } //finished not a protein product
                }
            }//finished cycling through features
        } //finished countup of bases

        output.add("ORIGIN");
        SequenceDNA seq = contig.getSequence();
        for (int i = 0; i < contig.getLength(); i += 60) {
            String line = "" + (i + 1) + "\t";
            for (int j = i; j < i + 60; j += 10) {
                if (j > contig.getLength() - 1)
                    break;
                int end = j + 9;
                if (end > contig.getLength() - 1)
                    end = contig.getLength() - 1;
                line += seq.getLinearSubSequence(new Position(j, end, false)).toString() + " ";

            }
            output.add(line);
        }


        try {
            PrintWriter schreiber = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            for (String s : output)
                schreiber.println(s);
            schreiber.close();
        } catch (IOException ex) {
            Logger.getLogger(makeGenebank.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

}
