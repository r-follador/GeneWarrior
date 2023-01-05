/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.annotation;

import com.genewarrior.genetools.sequenceHandling.Position;

import java.io.Serializable;

/**
 * @author btbase
 */
public class SoftberryORF implements Serializable {
    private static final long serialVersionUID = 9003673276777664518L;


    public String rastfeature;
    public Position pos;

    public SoftberryORF(String rastfeature, Position pos) {
        this.rastfeature = rastfeature;
        this.pos = pos;
    }


}
