/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.alignment;

public class SubstitutionMatrix {
    public final static byte AA = 0;
    public final static byte DNA = 1;

    public byte type = 0;
    public String name = "";
    public int distance = 0;

    public int[][] matrix;
    public int gapcost = 0;
}
