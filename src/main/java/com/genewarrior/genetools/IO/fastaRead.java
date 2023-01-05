/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.IO;

import com.genewarrior.genetools.Contig;
import com.genewarrior.genetools.GeneWarriorElement;
import com.genewarrior.genetools.Species;
import com.genewarrior.genetools.additionalInfo;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;

import java.io.*;
import java.util.ArrayList;

/**
 * @author kingcarlxx
 */
public class fastaRead {
    Species species;

    public Species getSpecies() {
        System.out.println("Species: " + species.name);
        for (Contig cont : species.contigs)
            System.out.println("Contig: " + cont.getName() + "; Size: " + cont.getLength());
        return species;
    }

    public fastaRead(File f, Species spec) throws IOException {
        if (spec == null) {
            species = new Species();
            species.name = "New Species";
            if (!species.setShortcut("@S" + GeneWarriorElement.generateShortcutNumber() + "@"))
                System.err.println("Shortcut for species didnt work!");
        } else
            species = spec;

        FileReader leser = new FileReader(f);
        BufferedReader reader = new BufferedReader(leser);
        ArrayList<String> contigName = new ArrayList<String>();
        ArrayList<String> sequence = new ArrayList<String>();

        try {
            String output;
            while ((output = reader.readLine()) != null) {
                if (output.startsWith(">")) {
                    contigName.add(output.substring(1));
                    sequence.add("");
                } else if (!output.trim().isEmpty()) {
                    if (sequence.isEmpty()) {
                        contigName.add("unnamed");
                        sequence.add("");
                    }
                    sequence.set((sequence.size() - 1), sequence.get(sequence.size() - 1) + output.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        reader.close();
        leser.close();

        for (int i = 0; i < contigName.size(); i++) {
            SequenceDNA seq = new SequenceDNA(sequence.get(i), false);
            seq.killGaps();
            Contig cont = new Contig();
            cont.setName(contigName.get(i));
            cont.setNote("");
            cont.setNumberingStart(1);
            cont.setParent(species);
            cont.setSequence(seq);
            cont.setadditionalInfo(new additionalInfo());

            if (!cont.setShortcut("@C" + GeneWarriorElement.generateShortcutNumber() + "@"))
                System.err.println("Shortcut didnt work!");

            species.addContig(cont);

        }
    }


}
