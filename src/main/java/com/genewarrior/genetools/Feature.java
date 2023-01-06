package com.genewarrior.genetools;

import com.genewarrior.genetools.sequenceHandling.*;

import java.util.Arrays;


/**
 * Feature class for MoleculeDNA. Contains type (String), name (String[2]), and <code>Position</code> of a single Feature. If Feature is a Protein-coding segment add also <code>proteinProduct</code>. A note (String) and <code>additionalInfo</code> can also be added. Everything is added using the constructor.
 *
 */
public class Feature extends GeneWarriorElement implements Cloneable {

    /**
     *
     */
    private static final long serialVersionUID = 604094944740974993L;
    public String type = "";
    public String[] name = new String[2];
    public Position position;
    public String note = "";

    private proteinProduct ProteinProperties;

    public additionalInfo properties;
    public additionalInfo links;

    public String tags = "";

    public void setProteinProduct(proteinProduct protProd) {
        ProteinProperties = protProd;
        ProteinProperties.setParent(this);
    }

    public proteinProduct getProteinProduct() {
        return ProteinProperties;
    }

    public boolean setShortcut(String sc) {
        if (!sc.matches("@F\\d{8}@"))
            return false;
        shortcut = sc;
        return true;
    }


    @Override
    public Object clone() {
        try {
            Feature cloned = (Feature) super.clone();
            cloned.position = (Position) position.clone();
            if (this.isProteinProduct())
                cloned.ProteinProperties = (proteinProduct) ProteinProperties.clone();
            cloned.properties = (additionalInfo) properties.clone();
            if (this.links != null)
                cloned.links = (additionalInfo) links.clone();
            cloned.tags = tags;
            String[] cloneName = new String[2];
            cloneName[0] = name[0];
            cloneName[1] = name[1];

            cloned.name = cloneName;
            if (cloned == null)
                System.out.println("clone is null");

            return cloned;
        } catch (CloneNotSupportedException e) {
            System.out.println("CloneNotSupportedException");
            return null;
        }
    }


    /**
     * Sets the protein SequenceAA
     *
     * @param seq entire SequenceDNA of parent
     */
    public void buildProtein(SequenceDNA seq) {
        ProteinProperties.setSequence(SequenceTools.DNA2Protein(seq.getLinearSubSequence(position), ProteinProperties.getTranslTable(), ProteinProperties.getCodonStart()));
        if (ProteinProperties.getSequence() == null) {
            ProteinProperties.setSequence(new SequenceAA(""));
            return;
        }
        byte[] seqarray = ProteinProperties.getSequence().getByteArray();
        seqarray[0] = Aminoacids._M; //set first Aa to M
        if (seqarray[seqarray.length - 1] == Aminoacids._TERM) //Remove the terminator (*)
            seqarray = Arrays.copyOf(seqarray, seqarray.length - 1);
        ProteinProperties.getSequence().setByteArray(seqarray);
    }


    public Feature(String type, String[] name, Position position, String note, proteinProduct ProteinProperties, additionalInfo properties) {
        this.type = type;
        this.name = name;
        this.position = position;
        this.note = note;
        this.properties = properties;
        this.ProteinProperties = ProteinProperties;
    }

    /**
     * Use to check if feature is a protein-encoding gene.
     *
     * @return true if feature is a protein-encoding gene, in this case <code>ProteinProperties</code>-field is not null.
     */
    public boolean isProteinProduct() {
        return ProteinProperties != null;
    }

    /**
     * Use to check if another <code>Feature</code> is the same as this one. (Checks Position, Type, Name)
     *
     * @param otherObject
     * @return
     */
    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) return true;
        if (otherObject == null) return false;
        if (getClass() != otherObject.getClass()) return false;

        Feature feature2 = (Feature) otherObject;

        return ((this.position.equals(feature2.position)) && (this.type.equals(feature2.type)) && (this.name[0].equals(feature2.name[0])) && (this.name[1].equals(feature2.name[1])));
    }

    /**
     * Checks for a given bp-positon (pos) if it is inside this feature
     *
     * @param pos
     * @param isCircular
     * @return
     */
    public boolean isIntegerInside(int pos, boolean isCircular) {
        return this.position.isIntegerInside(pos);
    }

    /**
     * add a Position to this feature
     *
     * @param pos
     */
    public void setPosition(Position pos) {
        this.position = pos;
    }

    public byte getObjectType() {
        return GeneWarriorElement.Feature;
    }
}
