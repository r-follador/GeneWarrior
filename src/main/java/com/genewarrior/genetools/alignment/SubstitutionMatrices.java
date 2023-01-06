/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.alignment;

/**
 * Data obtained from ftp://ftp.ncbi.nih.gov/blast/matrices/
 *
 */
public final class SubstitutionMatrices {

    public SubstitutionMatrix[] blosum = new SubstitutionMatrix[16];
    public SubstitutionMatrix[] pam = new SubstitutionMatrix[25];
    public SubstitutionMatrix dna = new SubstitutionMatrix();

    public SubstitutionMatrices() {
        dna = new SubstitutionMatricesDNA().dna;
        SubstitutionMatricesBlosum1 blosum1 = new SubstitutionMatricesBlosum1();
        SubstitutionMatricesBlosum2 blosum2 = new SubstitutionMatricesBlosum2();
        SubstitutionMatricesBlosum3 blosum3 = new SubstitutionMatricesBlosum3();
        System.arraycopy(blosum1.blosum, 0, blosum, 0, 7);
        System.arraycopy(blosum2.blosum, 7, blosum, 7, 5);
        System.arraycopy(blosum3.blosum, 12, blosum, 12, 4);

        SubstitutionMatricesPAM1 pam1 = new SubstitutionMatricesPAM1();
        SubstitutionMatricesPAM2 pam2 = new SubstitutionMatricesPAM2();
        SubstitutionMatricesPAM3 pam3 = new SubstitutionMatricesPAM3();
        SubstitutionMatricesPAM4 pam4 = new SubstitutionMatricesPAM4();
        SubstitutionMatricesPAM5 pam5 = new SubstitutionMatricesPAM5();

        System.arraycopy(pam1.pam, 0, pam, 0, 6);
        System.arraycopy(pam2.pam, 6, pam, 6, 7);
        pam[13] = pam3.pam[13];
        System.arraycopy(pam4.pam, 14, pam, 14, 6);
        System.arraycopy(pam5.pam, 20, pam, 20, 5);
    }
}
