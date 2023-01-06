package com.genewarrior.genetools;

import java.util.ArrayList;

public class Species extends GeneWarriorElement {
    private static final long serialVersionUID = 604094988740974903L;
    public String name = "blabla12";
    public String shortname = "blabla";

    public ArrayList<Contig> contigs = new ArrayList<Contig>();


    public boolean setShortcut(String sc) {
        if (!sc.matches("@S\\d{8}@"))
            return false;
        shortcut = sc;
        return true;
    }

    @Override
    public GeneWarriorElement getParent() {
        return null;
    }

    public void addContig(Contig contig) {
        contig.setParent(this);
        contigs.add(contig);
    }

    public byte getObjectType() {
        return GeneWarriorElement.Species;
    }

}
