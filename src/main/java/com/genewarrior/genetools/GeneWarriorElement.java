/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools;

import java.io.Serializable;
import java.util.Random;

/**
 * @author btserver
 */
public abstract class GeneWarriorElement implements Cloneable, Serializable {
    protected String shortcut;
    public final static byte Species = 1;
    public final static byte Contig = 2;
    public final static byte Feature = 3;
    public final static byte ProteinProduct = 4;
    protected GeneWarriorElement parent;

    /**
     * What type of Object is this <code>GeneWarriorElement</code>? Species, Contig, Feature or ProteinProduct
     *
     * @return
     */
    public abstract byte getObjectType();

    /**
     * Returns the associated parent. E.g. the Feature for the ProteinProduct, the Contig for the Feature or the Species for the Contig.
     * A species returns null.
     *
     * @return
     */
    public GeneWarriorElement getParent() {
        return parent;
    }

    public void setParent(GeneWarriorElement parent) {
        this.parent = parent;
    }

    public GeneWarriorElement() {
    }

    public String getShortcut() {
        return shortcut;
    }

    public abstract boolean setShortcut(String sc);

    /**
     * Creates randomly a 8digit shortcut number
     *
     * @return
     */
    public static String generateShortcutNumber() {
        String output = "";
        Random rnd = new Random();
        for (int i = 0; i < 8; i++) {
            output = output + rnd.nextInt(10);
        }
        return output;
    }

}
