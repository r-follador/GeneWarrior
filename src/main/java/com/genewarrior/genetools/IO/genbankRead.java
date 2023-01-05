/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.IO;

import com.genewarrior.genetools.*;
import com.genewarrior.genetools.sequenceHandling.Position;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;
import com.genewarrior.genetools.sequenceHandling.SequenceTools;

import java.io.*;
import java.util.ArrayList;

/**
 * @author kingcarlxx
 */
public class genbankRead {
    Species species = new Species();
    Contig convertedMolecule = new Contig();
    SequenceDNA convertedSequence = new SequenceDNA();

    private final static byte Headers = 0;
    private final static byte Features = 1;
    private final static byte Sequence = 2;
    private final static byte End = 3;
    private byte currentState = Headers;


    private boolean LocusHandled = false;

    Feature workingFeature = null;
    proteinProduct workingProteinProduct = null;

    ArrayList StringStore = new ArrayList();

    private void addLine(String str)

//ftp://ftp.ncbi.nih.gov/genbank/gbrel.txt    suacha noch "3.4.4 LOCUS Format"
//http://www.ncbi.nlm.nih.gov/Sitemap/samplerecord.html#MoleculeTypeB
    {
        if (currentState == Headers) {
            if (str.toLowerCase().trim().startsWith("locus")) //First line supposed to be locus
            {
                addLocusInformation(str);
            } else if (str.toLowerCase().trim().startsWith("features")) //features start
            {
                addHeaders(StringStore.toArray());
                StringStore = new ArrayList(); //delete old StringStore
                currentState = Features;
            } else if (LocusHandled) //muss nach Locus-Entry sein
            {
                StringStore.add(str);
            }
        } else if (currentState == Features) {
            if (str.toLowerCase().trim().startsWith("origin")) //sequence is starting
            {
                addFeature(StringStore.toArray());
                StringStore = new ArrayList(); //delete old StringStore
                currentState = Sequence;
            } else {
                if (str.substring(0, 20).trim().isEmpty()) //Weiterführung eines Features
                    StringStore.add(str);
                else //neues Feature
                {
                    addFeature(StringStore.toArray());
                    StringStore = new ArrayList(); // delete old StringStore
                    StringStore.add(str);
                }
            }
        } else if (currentState == Sequence) {
            if (str.substring(0, 2).trim().isEmpty()) {
                addSequence(str);
            } else if (str.startsWith("//"))//sequence ist fertig, nächste beginnt
            {
                finishMolecule();
                currentState = Headers;
                convertedMolecule = new Contig();
                convertedSequence = new SequenceDNA();

                LocusHandled = false;

                workingFeature = null;
                workingProteinProduct = null;
                StringStore = new ArrayList();
            } else {
                currentState = End;
            }
        } else if (currentState == End) {
            //chill&do nutting
        }
    }

    private void addLocusInformation(String str) {
        //Todo: should be tokenized, in case spacing will ever change
        if (!str.substring(12, 27).trim().isEmpty())
            convertedMolecule.getadditionalInfo().add("GenBank Locus Information", "Locus Name", str.substring(12, 27).trim());
        if (!str.substring(29, 39).trim().isEmpty())
            convertedMolecule.getadditionalInfo().add("GenBank Locus Information", "Length (bp)", str.substring(29, 40).trim());
        if (!str.substring(44, 52).trim().isEmpty()) {
            convertedMolecule.getadditionalInfo().add("GenBank Locus Information", "Type", str.substring(44, 52).trim());
            if (str.substring(44, 52).toLowerCase().contains("rna"))
                convertedSequence.convert2RNA();
            else
                convertedSequence.convert2DNA();
        } else //assume DNA if not specified
            convertedSequence.convert2DNA();
        if (!str.substring(55, 62).trim().isEmpty()) {
            convertedMolecule.getadditionalInfo().add("GenBank Locus Information", "Form", str.substring(55, 63).trim());
        }
        if (!str.substring(64, 67).trim().isEmpty()) {
            String div = str.substring(64, 67).trim();
            String division = div;
            if (div.equalsIgnoreCase("pri")) division = "Primate Sequence";
            else if (div.equalsIgnoreCase("rod")) division = "Rodent Sequence";
            else if (div.equalsIgnoreCase("mam"))
                division = "Other Mammalian Sequence (neither Primate nor Rodent)";
            else if (div.equalsIgnoreCase("vrt")) division = "Other Vertebrate Sequence (not Mammalian)";
            else if (div.equalsIgnoreCase("inv")) division = "Invertebrate Sequence";
            else if (div.equalsIgnoreCase("pln")) division = "Plant, Fungal, and Algal Sequence";
            else if (div.equalsIgnoreCase("bct")) division = "Bacterial Sequence";
            else if (div.equalsIgnoreCase("vrl")) division = "Viral Sequence";
            else if (div.equalsIgnoreCase("phg")) division = "Bacteriophage Sequence";
            else if (div.equalsIgnoreCase("syn")) division = "Synthetic Sequence";
            else if (div.equalsIgnoreCase("una")) division = "Unannotated Sequence";
            else if (div.equalsIgnoreCase("est")) division = "EST (expressed sequence tags) Sequence";
            else if (div.equalsIgnoreCase("pat")) division = "Patent Sequence";
            else if (div.equalsIgnoreCase("sts")) division = "STS (sequence tagged sites) Sequence";
            else if (div.equalsIgnoreCase("gss")) division = "GS (genome survey) Sequence";
            else if (div.equalsIgnoreCase("htg")) division = "HTG (high-throughput) Sequence";
            else if (div.equalsIgnoreCase("htc")) division = "HTC (unfinished high-throughput cDNA) Sequence";
            else if (div.equalsIgnoreCase("env")) division = "Environmental Sampling Sequence";

            convertedMolecule.getadditionalInfo().add("GenBank Locus Information", "Sequence Type", division);
        }

        if (str.length() > 79 && !str.substring(68, 79).trim().isEmpty())
            convertedMolecule.getadditionalInfo().add("GenBank Locus Information", "Date of Modification", str.substring(68, 79).trim());

        LocusHandled = true;
    }

    private void addHeaders(Object[] str) {
        int numberoflines = str.length;
        int nrofReferences = 0;
        for (int i = 0; i < numberoflines; i++) {
            String line = (String) str[i];

            //Read Definition, set as Name of Molecule
            if (line.toLowerCase().trim().startsWith("definition")) {
                String definition = line.substring(12).trim();
                int nextlines = i + 1;
                while (nextlines < numberoflines && ((String) str[nextlines]).substring(0, 11).trim().isEmpty()) //advance until next keyword appears
                {
                    definition = definition + " " + ((String) str[nextlines]).substring(12).trim();
                    i = nextlines;
                    nextlines++;

                }

                convertedMolecule.setName(definition);
            }

            //Read Accession
            else if (line.toLowerCase().trim().startsWith("accession")) {
                String accession = line.substring(12).trim();
                int nextlines = i + 1;
                while (nextlines < numberoflines && ((String) str[nextlines]).substring(0, 11).trim().isEmpty()) //advance until next keyword appears
                {
                    accession = accession + " " + ((String) str[nextlines]).substring(12).trim();
                    i = nextlines;
                    nextlines++;

                }

                String[] tokens = accession.split("\\s");

                for (int j = 0; j < tokens.length; j++) {
                    if (j == 0)
                        convertedMolecule.getadditionalInfo().add("GenBank Accession Number", "Primary Accession Number", tokens[0]);
                    else if (!tokens[j].trim().isEmpty())
                        convertedMolecule.getadditionalInfo().add("GenBank Accession Number", "Secondary Accession Number", tokens[j]);
                }
            }

            //Read Version
            else if (line.toLowerCase().trim().startsWith("version")) {
                String version = line.substring(12).trim();
                String[] tokens = version.split("\\s");

                if (tokens.length > 0)
                    convertedMolecule.getadditionalInfo().add("GenBank Entry", "Compound Accession Number Version", tokens[0]);
                if (tokens.length > 1)
                    convertedMolecule.getadditionalInfo().add("GenBank Entry", "NCBI GI Identifier Version", tokens[tokens.length - 1]);
            }

            //Read DBLink
            else if (line.toLowerCase().trim().startsWith("dblink")) {
                String dblink = line.substring(12).trim();

                convertedMolecule.getadditionalInfo().add("GenBank Entry", "DBLink", dblink);
            }

            //Read Keywords
            else if (line.toLowerCase().trim().startsWith("keywords")) {
                String definition = line.substring(12).trim();
                int nextlines = i + 1;
                while (nextlines < numberoflines && ((String) str[nextlines]).substring(0, 11).trim().isEmpty()) //advance until next keyword appears
                {
                    definition = definition + " " + ((String) str[nextlines]).substring(12).trim();
                    i = nextlines;
                    nextlines++;

                }

                String[] tokens = definition.split(";");
                for (int j = 0; j < tokens.length; j++) {
                    if (!tokens[j].trim().isEmpty() && !tokens[j].trim().equals("."))
                        convertedMolecule.getadditionalInfo().add("GenBank Entry", "Keyword", tokens[j].trim());
                }
            }

            //Read Segment
            else if (line.toLowerCase().trim().startsWith("segment")) {
                String segment = line.substring(12).trim();

                convertedMolecule.getadditionalInfo().add("GenBank Entry", "Segment", segment);
            }

            //Read Source
            else if (line.toLowerCase().trim().startsWith("source")) {
                String definition = line.substring(12).trim();
                int nextlines = i + 1;
                while (nextlines < numberoflines && ((String) str[nextlines]).substring(0, 11).trim().isEmpty()) //advance until next keyword appears
                {
                    definition = definition + " " + ((String) str[nextlines]).substring(12).trim();
                    i = nextlines;
                    nextlines++;

                }

                convertedMolecule.getadditionalInfo().add("Source", "Organism name/Molecule Type", definition);
            }

            //Read Organism
            else if (line.toLowerCase().trim().startsWith("organism")) {
                String definition1 = line.substring(12).trim();
                String definition2 = "";
                int nextlines = i + 1;
                while (nextlines < numberoflines && ((String) str[nextlines]).substring(0, 11).trim().isEmpty()) //advance until next keyword appears
                {
                    definition2 = definition2 + " " + ((String) str[nextlines]).substring(12).trim();
                    i = nextlines;
                    nextlines++;

                }

                convertedMolecule.getadditionalInfo().add("Source", "Source Organism (scientific name)", definition1);
                convertedMolecule.getadditionalInfo().add("Source", "Taxonomic Classification Levels", definition2);
            }

            //Reference
            else if (line.toLowerCase().trim().startsWith("reference")) {
                nrofReferences++;
                String reference = line.substring(11).trim();
                convertedMolecule.getadditionalInfo().add("Reference #" + nrofReferences, "Number (range of bases)", reference);

                int nextlines = i + 1;
                while (nextlines < numberoflines && ((String) str[nextlines]).substring(0, 2).trim().isEmpty()) //advance until next keyword appears
                {
                    String title = ((String) str[nextlines]).substring(0, 11).trim();
                    if (title.length() > 2)
                        title = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
                    String subreference = ((String) str[nextlines]).substring(12).trim();
                    int ubernextlines = nextlines + 1;
                    while (ubernextlines < numberoflines && ((String) str[ubernextlines]).substring(0, 11).trim().isEmpty()) //advance until next keyword appears
                    {
                        subreference = subreference + " " + ((String) str[ubernextlines]).substring(12).trim();
                        nextlines = ubernextlines;
                        ubernextlines++;

                    }

                    convertedMolecule.getadditionalInfo().add("Reference #" + nrofReferences, title, subreference);
                    i = nextlines;
                    nextlines++;

                }


            }


        }
    }

    private void addFeature(Object[] str) {
        if (str.length == 0)
            return;

        String type = "";
        String[] name = new String[2];
        name[0] = "";
        name[1] = "";
        Position position;
        boolean isPosComplement = false;
        int firstPos;
        int lastPos;

        boolean isProteinProduct = false;

        String note = "";
        String product = "";
        String protein_id = "";
        int codonStart = 1;
        int translTable = 11;

        additionalInfo properties = new additionalInfo();

        //Use first line for Type and Location
        String firstLine = (String) str[0];
        type = firstLine.substring(0, 21).trim().toUpperCase();
        if (type.isEmpty()) {
            return;
        }

        if (type.equalsIgnoreCase("cds")) {
            isProteinProduct = true;
        }

        String location = firstLine.substring(21).trim();
        if (location.toLowerCase().startsWith("complement")) {
            isPosComplement = true;
            location = location.substring(11, location.length() - 1);
        }

        String[] token = location.split("\\u002E\\u002E"); //unicode for ".."


        if (token.length == 0)
            return;
        else if (token.length == 1) {
            try {
                firstPos = Integer.decode(token[0].trim()) - 1;
                position = new Position(firstPos);
            } catch (NumberFormatException e) {
                return;
            }
        } else if (token.length == 2) {
            try {
                firstPos = Integer.decode(token[0].trim()) - 1;
                lastPos = Integer.decode(token[1].trim()) - 1;

                position = new Position((firstPos < lastPos ? firstPos : lastPos), (firstPos < lastPos ? lastPos : firstPos), isPosComplement);
            } catch (NumberFormatException e) {
                return;
            }
        } else
            return;

        //Feature labels
        for (int i = 1; i < str.length; i++) {
            String line = (String) str[i];

            int nextlines = i + 1;
            while (nextlines < str.length && !((String) str[nextlines]).trim().startsWith("/")) //advance until next keyword appears
            {
                line += ((String) str[nextlines]).trim();

                i = nextlines;
                nextlines++;

            }

            String[] tokens = line.trim().split("=");

            if (tokens.length != 2)
                break;

            tokens[0] = tokens[0].substring(1);
            if (tokens[1].startsWith("\""))
                tokens[1] = tokens[1].substring(1, tokens[1].length() - 1);

            if (isProteinProduct) {
                //System.out.println("Tokens: "+tokens[0]+" Line: "+i);
                if (tokens[0].equalsIgnoreCase("gene"))
                    name[0] = tokens[1];
                else if (tokens[0].equalsIgnoreCase("product")) {
                    product = tokens[1];
                    //System.out.println("Recognized: "+product);
                } else if (tokens[0].equalsIgnoreCase("locus_tag")) {
                    name[1] = tokens[1];
                } else if (tokens[0].equalsIgnoreCase("codon_start")) {
                    try {
                        codonStart = Integer.decode(tokens[1]);
                    } catch (NumberFormatException e) {
                    }
                } else if (tokens[0].equalsIgnoreCase("transl_table")) {
                    try {
                        translTable = Integer.decode(tokens[1]);
                    } catch (NumberFormatException e) {
                    }
                } else if (tokens[0].equalsIgnoreCase("protein_id"))
                    protein_id = tokens[1];
                else if (tokens[0].equalsIgnoreCase("translation"))
                    ;
                else if (tokens[0].equalsIgnoreCase("note")) {
                    if (name[0].trim().isEmpty())
                        name[0] = tokens[1];
                    else
                        note = tokens[1];
                } else {
                    properties.add("Genbank Label", tokens[0], tokens[1]);
                }
            } else //not proteinproduct
            {
                if (tokens[0].equalsIgnoreCase("gene"))
                    name[0] = tokens[1];
                else if (tokens[0].equalsIgnoreCase("product"))
                    name[1] = tokens[1];
                else if (tokens[0].equalsIgnoreCase("locus_tag")) {
                    if (name[1].isEmpty())
                        name[1] = tokens[1];
                } else if (tokens[0].equalsIgnoreCase("note")) {
                    if (name[0].trim().isEmpty())
                        name[0] = tokens[1];
                    else
                        note = tokens[1];
                } else {
                    properties.add("Genbank Label", tokens[0], tokens[1]);
                }
            }//decision if cds or not
        }//end feature labels
        Feature neuesFeature;

        if (isProteinProduct) {
            proteinProduct ProteinProperties = new proteinProduct(name[0], product, protein_id, codonStart, translTable);
            ProteinProperties.setShortcut("@P" + getShortcutNumber() + "@");
            neuesFeature = new Feature(type, name, position, note, ProteinProperties, properties);
        } else
            neuesFeature = new Feature(type, name, position, note, null, properties);
        neuesFeature.setShortcut("@F" + getShortcutNumber() + "@");
        convertedMolecule.addFeature(neuesFeature);

        //hier weiter
    }

    private void addSequence(String str) {
        String sequence = str.substring(10).trim();
        sequence = sequence.replace(" ", "");
        SequenceDNA sequence2add = new SequenceDNA(sequence, convertedSequence.isRNA());
        convertedSequence = SequenceTools.combineSequences(convertedSequence, sequence2add);
    }

    private void finishMolecule() {
        convertedMolecule.setSequence(convertedSequence);
        convertedMolecule.setShortcut("@C" + getShortcutNumber() + "@");

        for (Feature f : convertedMolecule.featureSet) {
            if (f.isProteinProduct()) {
                //System.out.println(f.name[0]+"/"+f.name[1]);
                f.buildProtein(convertedMolecule.getSequence());
            }
        }
        species.addContig(convertedMolecule);


    }

    public Species getSpecies() {

        species.setShortcut("@S" + getShortcutNumber() + "@");
        return species;
    }


    public genbankRead(File f) throws IOException {
        FileReader leser = new FileReader(f);
        BufferedReader reader = new BufferedReader(leser);
        int linenr = 0;

        try {
            String output;
            while ((output = reader.readLine()) != null) {
                linenr++;
                if (linenr % 10000 == 0)
                    System.out.println(linenr);
                if (!output.isEmpty())
                    addLine(output);
            }
            finishMolecule();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        reader.close();
        leser.close();
    }

    private static String getShortcutNumber() {
        return GeneWarriorElement.generateShortcutNumber();
    }


}
