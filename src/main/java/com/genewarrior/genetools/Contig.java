/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools;

import com.genewarrior.genetools.sequenceHandling.Position;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Either DNA- or RNA-molecule. Use <code>getType()</code> to assess which one it is
 *
 * @author kingcarlxx
 */
public class Contig extends GeneWarriorElement implements Cloneable {
    /**
     *
     */
    private static final long serialVersionUID = 7689206690960653792L;
    private SequenceDNA sequence;
    protected additionalInfo moreInfos = new additionalInfo();
    private String name = "Unnamed Molecule";
    private String note = "";
    public ArrayList<Feature> featureSet = new ArrayList<Feature>();

    private int setNumberingStart = 1;

    public boolean setShortcut(String sc) {
        if (!sc.matches("@C\\d{8}@"))
            return false;
        shortcut = sc;
        return true;
    }

    public void eraseFeatures() {
        featureSet = new ArrayList<Feature>();
    }

    /**
     * Deep clone of MoleculeDNA
     */
    @Override
    public Object clone() {
        try {
            Contig cloned = (Contig) super.clone();
            cloned.sequence.setByteArray(sequence.getByteArray().clone());
            cloned.setNumberingStart(setNumberingStart);
            cloned.name = name;
            cloned.note = note;
            cloned.moreInfos = (additionalInfo) moreInfos.clone();
            cloned.eraseFeatures();

            for (Feature f : this.featureSet)
                cloned.addFeature((Feature) f.clone());

            return cloned;
        } catch (CloneNotSupportedException e) {
            return null;
        }

    }

    public int[] getCodingFeaturesIndices() {
        ArrayList<Integer> liste = new ArrayList<Integer>();
        for (int i = 0; i < featureSet.size(); i++) {
            if (featureSet.get(i).isProteinProduct())
                liste.add(i);
        }

        int[] output = new int[liste.size()];
        for (int i = 0; i < liste.size(); i++) {
            output[i] = liste.get(i);
        }
        return output;
    }


    /**
     * Calculates Length of Upper Strand (regarding Sticky Ends)
     *
     * @return
     */
    public int getLength() {
        return sequence.getLength();
    }


    /**
     * Converts given bp-Position (internal numbering) to UserInterface-bp-Position; count starts after stickyEnd of upper Strand and first basepair number according to NumberingScheme
     *
     * @param bp
     * @return
     */
    public int convert2NumberingScheme(int bp) {
        return bp + setNumberingStart;
    }

    /**
     * Converts given UserInterface-Postion to internal numbering
     *
     * @param pos
     * @return
     */
    public int convert2InternalScheme(int pos) {
        return pos - setNumberingStart;
    }

    /**
     * Sets the Numbering scheme of the single nucleotides
     *
     * @param FirstBp Number of the first Nucleotide (normally 1); ignore sticky ends
     */
    public void setNumberingStart(int FirstBp) {
        this.setNumberingStart = FirstBp;
    }

    /**
     * Gets the number of the first basepair according to the numbering scheme
     *
     * @return number of first nucleotide
     */
    public int getNumberingStart() {
        return setNumberingStart;
    }

    /**
     * Returns ArrayList with Features of this MoleculeDNA which lie completely inside given segment
     *
     * @param segment
     * @return
     */
    public ArrayList<Feature> getFeaturesInsideSegment(Position segment) {
        if (!isPositionLegal(segment))
            return null;
        ArrayList<Feature> features = new ArrayList<Feature>();


        for (Object o : featureSet) {
            Feature f = (Feature) o;
            if (segment.isPositionInside(f.position))
                features.add(f);
        }

        return features;
    }

    /**
     * Returns ArrayList with Features of this MoleculeDNA touching given integer (position)
     *
     * @param pos
     * @return
     */
    public ArrayList<Feature> getFeaturesTouchingPosition(int pos) {
        if (!isPositionLegal(new Position(pos)))
            return null;
        ArrayList<Feature> features = new ArrayList<Feature>();


        for (Object o : featureSet) {
            Feature f = (Feature) o;
            if (f.position.isIntegerInside(pos))
                features.add(f);
        }

        return features;
    }


    /**
     * Use to check if <code>Position</code> is legal to use in the context of this molecule
     *
     * @param pos
     * @return true if <code>Position</code> is inside the boundaries of this molecule
     */
    public boolean isPositionLegal(Position pos) {
        return pos.getStartPos() <= sequence.getLength() - 1 && pos.getEndPos() <= sequence.getLength() - 1 && pos.getEndPos() >= pos.getStartPos();
    }

    /**
     * @return "rna" if molecule is a rna-molecule, "dna" if molecule is a dna-molecule. (Actually returns .isRNA() of SequenceDNA)
     */
    public String getType() {
        if (sequence.isRNA())
            return "rna";
        else
            return "dna";
    }

    /**
     * @return true if molecule is RNA
     */
    public boolean isRNA() {
        return sequence.isRNA();
    }

    /**
     * @return <code>SequenceDNA</code> of this molecule
     */
    public SequenceDNA getSequence() {
        return sequence;
    }

    /**
     * Set <code>SequenceDNA</code> of this molecule, use also to set if RNA or DNA
     *
     * @param sequence
     */
    public void setSequence(SequenceDNA sequence) {
        this.sequence = sequence;
    }

    /**
     * Returns a feature with given index
     *
     * @param index
     * @return
     */
    public Feature getFeature(int index) {
        return featureSet.get(index);
    }


    /**
     * Get the <code>Iterator</code> to circle through all the <code>>Feature</code>s
     *
     * @return
     */
    public Iterator<Feature> getFeatureIterator() {
        return featureSet.iterator();
    }

    /**
     * Return number of features in this molecule
     *
     * @return
     */
    public int getFeatureLength() {
        return featureSet.size();
    }


    /**
     * add new <code>Feature</code> to this molecule
     *
     * @param feature
     * @return true if addition of <code>Feature</code> to <code>Set</code> was successful, false if <code>Feature</code> is already present.
     */
    public boolean addFeature(Feature feature) {
        if (feature == null)
            return false;
        feature.setParent(this);
        return featureSet.add(feature);
    }

    /**
     * Set <code>additionalInfo</code> for this molecule.
     *
     * @param moreInfos
     */
    public void setadditionalInfo(additionalInfo moreInfos) {
        this.moreInfos = moreInfos;
    }

    /**
     * Get <code>additionalInfo</code> of this molecule.
     *
     * @return
     */
    public additionalInfo getadditionalInfo() {
        return moreInfos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public byte getObjectType() {
        return GeneWarriorElement.Contig;
    }
}
