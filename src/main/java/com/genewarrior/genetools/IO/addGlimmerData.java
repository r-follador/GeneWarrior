/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.IO;


import com.genewarrior.genetools.*;
import com.genewarrior.genetools.sequenceHandling.Position;

import java.io.*;

/**
 * @author kingcarlxx
 */
public class addGlimmerData {
    Species species;

    public Species getSpecies() {
        return species;
    }

    public addGlimmerData(File f, Species spec) throws IOException {
        if (spec == null) {
            System.err.println("No species!");
            return;
        } else
            species = spec;

        FileReader leser = new FileReader(f);
        BufferedReader reader = new BufferedReader(leser);

        Contig handledContig = null;

        try {

            String output;
            while ((output = reader.readLine()) != null) {
                if (output.startsWith("GLIMMER") || output.startsWith(" orfID") || output.startsWith("----")) {
                } else {
                    if (output.startsWith(">")) {
                        for (Contig contig : species.contigs) {
                            if (contig.getName().toLowerCase().trim().startsWith(output.substring(1).trim().toLowerCase()))
                                handledContig = contig;
                        }
                    } else if (output.startsWith("orf") && !(handledContig == null)) {
                        output = output.trim();
                        String[] split = output.split("\\s+");
                        if (split.length == 5) {
                            String[] name = new String[2];
                            name[0] = split[0];
                            name[1] = "";
                            String type = "ORF";

                            int start = Integer.decode(split[1]) - 1;
                            int stop = Integer.decode(split[2]) - 1;

                            boolean isReverse = false;

                            if (start > stop) {
                                int buffer = start;
                                start = stop;
                                stop = buffer;
                                isReverse = true;
                            }

                            String note = "Glimmer Score: " + split[4];


                            proteinProduct prod = new proteinProduct(name[0], "Glitter-Predicted Protein", "", 1, 11);
                            prod.setShortcut("@P" + GeneWarriorElement.generateShortcutNumber() + "@");
                            Feature feat = new Feature(type, name, new Position(start, stop, isReverse), note, prod, new additionalInfo());
                            System.out.println(feat.name[0] + " " + feat.position.getStartPos() + " " + feat.position.getEndPos() + " - " + handledContig.getName());
                            feat.buildProtein(handledContig.getSequence());
                            feat.getProteinProduct().setParent(feat);
                            feat.setParent(handledContig);
                            feat.setShortcut("@F" + GeneWarriorElement.generateShortcutNumber() + "@");

                            handledContig.addFeature(feat);
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        reader.close();
        leser.close();


    }


}
