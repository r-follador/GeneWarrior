/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools;


import com.genewarrior.genetools.sequenceHandling.SequenceAA;

/**
 * A <code>feature</code> (gene) which encodes a Protein posesses an entry of this class.
 *
 * @author kingcarlxx
 */
public class proteinProduct extends GeneWarriorElement implements Cloneable {

    /**
     *
     */
    private static final long serialVersionUID = 4254387541178327615L;
    String name;
    String product;
    String protein_id;
    int codonStart;
    int translTable;
    private SequenceAA sequence;

    public boolean setShortcut(String sc) {
        if (!sc.matches("@P\\d{8}@"))
            return false;
        shortcut = sc;
        return true;
    }

    /**
     * @param name        name of gene
     * @param product     name of product (enzyme name)
     * @param protein_id  ID of protein
     * @param codonStart  at which nt does the codon start (frame shift) (1, 2 or 3)
     * @param TranslTable which translation table is to be used
     */
    public proteinProduct(String name, String product, String protein_id, int codonStart, int TranslTable) {
        this.name = name;
        this.product = product;
        this.protein_id = protein_id;
        this.codonStart = codonStart;
        this.translTable = TranslTable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public String getProteinID() {
        return protein_id;
    }

    public int getCodonStart() {
        return codonStart;
    }

    public int getTranslTable() {
        return translTable;
    }

    public void setSequence(SequenceAA sequence) {
        this.sequence = sequence;
    }

    public SequenceAA getSequence() {
        return sequence;
    }

    @Override
    public Object clone() {
        try {
            proteinProduct clone = (proteinProduct) super.clone();
            clone.sequence.setByteArray(sequence.getByteArray().clone());
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public byte getObjectType() {
        return GeneWarriorElement.ProteinProduct;
    }
}
