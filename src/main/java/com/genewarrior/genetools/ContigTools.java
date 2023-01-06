package com.genewarrior.genetools;

import com.genewarrior.genetools.sequenceHandling.Position;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;
import com.genewarrior.genetools.sequenceHandling.SequenceTools;

import java.util.ArrayList;


/**
 * Methods to handle <code>MoleculeDNA</code> Objects. All static methods.
 *
 */
public class ContigTools {


    /**
     * Ligates right-end of molecule1 to left-end of molecule2. Does not check if ligation is biochemically possible.
     *
     * @param molecule1
     * @param molecule2
     * @return
     */
    public static Contig ligate(Contig molecule1, Contig molecule2) {
        Contig ligated = new Contig();
        ligated.setName(molecule1.getName() + " + " + molecule2.getName());
        ligated.setNote("Ligation of " + molecule1.getName() + " + " + molecule2.getName() + "; " + molecule1.getName() + ": " + molecule1.getNote() + " ;" + molecule2.getName() + ": " + molecule2.getNote());
        ligated.setNumberingStart(molecule1.getNumberingStart());

        additionalInfo moreInfo1 = molecule1.getadditionalInfo();
        for (int i = 0; i < moreInfo1.getSize(); i++) {
            ligated.moreInfos.add(molecule1.getName() + ": " + moreInfo1.headers.get(i), moreInfo1.titles.get(i), moreInfo1.texts.get(i));
        }

        additionalInfo moreInfo2 = molecule2.getadditionalInfo();
        for (int i = 0; i < moreInfo2.getSize(); i++) {
            ligated.moreInfos.add(molecule2.getName() + ": " + moreInfo2.headers.get(i), moreInfo2.titles.get(i), moreInfo2.texts.get(i));
        }

        ligated.moreInfos.add("Modification History", "Ligation", "Product of Ligation of " + molecule1.getName() + " and " + molecule2.getName());


        int offset = 0;

        SequenceDNA ligatedSequence = SequenceTools.combineSequences(molecule1.getSequence(), molecule2.getSequence().getLinearSubSequence(new Position(offset, molecule2.getSequence().getLength() - 1, false)));

        ligated.setSequence(ligatedSequence);
        int featureOffset = molecule1.getSequence().getLength() - offset;

        for (Feature f : molecule1.featureSet) {
            ligated.addFeature((Feature) f.clone());
        }

        for (Feature f : molecule2.featureSet) {
            Feature g = (Feature) f.clone();
            g.position = new Position(f.position.getStartPos() + featureOffset, f.position.getEndPos() + featureOffset, f.position.isReverse());
            ligated.addFeature(g);
        }

        return ligated;
    }


    /**
     * Switches Upper with Lower Strand. Essentially rotates a linear DNA/RNA fragment, and switches strands in circular molecules<br>
     * The physical molecule remains unchanged. Can only be applied to Doublestranded Molecules.
     *
     * @param molecule
     * @return
     */
    public static Contig switchStrands(Contig molecule) {

        Contig mirrored = (Contig) molecule.clone();

        mirrored.setSequence(SequenceTools.reverseComplement(molecule.getSequence(), true, true));
        mirrored.featureSet = new ArrayList<Feature>();

        int sequenceLength = molecule.getSequence().getLength();

        //Circle through every feature, change position and direction and add to mirrored
        for (Feature f : molecule.featureSet) {
            Feature g = (Feature) f.clone();
            g.setPosition(new Position(sequenceLength - f.position.getEndPos() - 1, sequenceLength - f.position.getStartPos() - 1, !f.position.isReverse()));
            mirrored.addFeature(g);
        }
        return mirrored;
    }
}
