/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.GenomeComparison;

import com.genewarrior.genetools.Contig;
import com.genewarrior.genetools.Feature;
import com.genewarrior.genetools.Species;
import com.genewarrior.genetools.alignment.AlignmentTools;
import com.genewarrior.genetools.alignment.SubstitutionMatrix;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author btbase
 */
public class ProteinScoreRatio implements Serializable {
    private static final long serialVersionUID = -6801098088168393223L;

    transient Species ReferenceSpec;
    transient Species QuerySpec;


    Map<String, Integer> ReferenceSc2Int = new HashMap<String, Integer>();
    Map<String, Integer> QuerySc2Int = new HashMap<String, Integer>();
    String[] ReferenceInt2Sc;
    String[] QueryInt2Sc;

    int[] selfAlignScore;
    int[][] AlignScore; //[Reference][Query]
    double[][] percentage; //[Reference][Query]

    public String[] getReferenceShorcutList() {
        return ReferenceInt2Sc;
    }

    public String[] getQueryShorcutList() {
        return QueryInt2Sc;
    }


    /**
     * Returns best match in Query for given Reference
     *
     * @param sc Shortcut of Reference Feature
     * @return
     */
    public String getBestMatchForReference(String sc) {
        Integer refindex = ReferenceSc2Int.get(sc);
        if (refindex == null)
            return null;

        int highscore = 0;
        int highscoreIndex = 0;
        for (int i = 0; i < QueryInt2Sc.length; i++) {
            if (highscore < AlignScore[refindex][i]) {
                highscore = AlignScore[refindex][i];
                highscoreIndex = i;
            }
        }

        return QueryInt2Sc[highscoreIndex];
    }

    /**
     * Returns best match in Reference for given Query
     *
     * @param sc Shortcut of Query Feature
     * @return
     */
    public String getBestMatchForQuery(String sc) {
        Integer queryindex = QuerySc2Int.get(sc);
        if (queryindex == null) {
            System.out.println("not in referencesc2int");
            return null;

        }

        int highscore = 0;
        int highscoreIndex = 0;
        for (int i = 0; i < ReferenceInt2Sc.length; i++) {
            if (highscore < AlignScore[i][queryindex]) {
                highscore = AlignScore[i][queryindex];
                highscoreIndex = i;
            }
        }

        return ReferenceInt2Sc[highscoreIndex];
    }

    public int getScore(String refSc, String querySc) {
        Integer refindex = ReferenceSc2Int.get(refSc);
        if (refindex == null)
            return -1;
        Integer queryindex = QuerySc2Int.get(querySc);
        if (queryindex == null)
            return -1;
        return AlignScore[refindex][queryindex];
    }


    public double getPercentage(String refSc, String querySc) {
        Integer refindex = ReferenceSc2Int.get(refSc);
        if (refindex == null)
            return 99;
        Integer queryindex = QuerySc2Int.get(querySc);
        if (queryindex == null)
            return 99;
        return percentage[refindex][queryindex];
    }

    public ProteinScoreRatio(Species Reference, Species Query) {
        ReferenceSpec = Reference;
        QuerySpec = Query;

        int ReferenceSize = 0;
        int QuerySize = 0;

        for (Contig c : ReferenceSpec.contigs) {
            for (Feature f : c.featureSet) {
                if (f.isProteinProduct())
                    ReferenceSize++;
            }
        }

        for (Contig c : QuerySpec.contigs) {
            for (Feature f : c.featureSet) {
                if (f.isProteinProduct())
                    QuerySize++;
            }
        }

        ReferenceInt2Sc = new String[ReferenceSize];
        QueryInt2Sc = new String[QuerySize];
        selfAlignScore = new int[ReferenceSize];
        AlignScore = new int[ReferenceSize][QuerySize];
        percentage = new double[ReferenceSize][QuerySize];

        int RefFeaturenr = -1;
        for (Contig c : ReferenceSpec.contigs) {
            for (Feature f : c.featureSet) {
                if (f.isProteinProduct()) {
                    RefFeaturenr++;
                    ReferenceSc2Int.put(f.getShortcut(), RefFeaturenr);
                    ReferenceInt2Sc[RefFeaturenr] = f.getShortcut();
                }
            }
        }

        int QueryFeaturenr = -1;
        for (Contig c : QuerySpec.contigs) {
            for (Feature f : c.featureSet) {
                if (f.isProteinProduct()) {
                    QueryFeaturenr++;
                    QuerySc2Int.put(f.getShortcut(), QueryFeaturenr);
                    QueryInt2Sc[QueryFeaturenr] = f.getShortcut();
                }
            }
        }
    }

    /**
     * Check if the Reference Species has any new Features
     *
     * @param sm
     * @param ReferenceSpecies
     * @param QuerySpecies
     */
    public void update(SubstitutionMatrix sm, Species ReferenceSpecies, Species QuerySpecies) {
        ReferenceSpec = ReferenceSpecies;
        QuerySpec = QuerySpecies;

        for (Contig c : ReferenceSpec.contigs) {
            for (Feature f : c.featureSet) {
                if (!ReferenceSc2Int.containsKey(f.getShortcut()) && f.isProteinProduct()) //shortcut doesn't exist, do the work
                {
                    System.out.println("Adding shortcut: " + f.getShortcut());
                    int existingFeatures = ReferenceInt2Sc.length;
                    ReferenceSc2Int.put(f.getShortcut(), existingFeatures);
                    String[] newReferenceInt2Sc = Arrays.copyOf(ReferenceInt2Sc, existingFeatures + 1);
                    newReferenceInt2Sc[existingFeatures] = f.getShortcut();
                    int[] newSelfAlignScore = Arrays.copyOf(selfAlignScore, existingFeatures + 1);
                    newSelfAlignScore[existingFeatures] = AlignmentTools.quickalign(f.getProteinProduct().getSequence(), f.getProteinProduct().getSequence(), sm, AlignmentTools.Local);
                    int[][] newAlignScore = new int[existingFeatures + 1][QueryInt2Sc.length]; //[Reference][Query]
                    double[][] newPercentage = new double[existingFeatures + 1][QueryInt2Sc.length];
                    for (int a = 0; a < existingFeatures; a++) {
                        for (int b = 0; b < QueryInt2Sc.length; b++) {
                            newAlignScore[a][b] = AlignScore[a][b];
                            newPercentage[a][b] = percentage[a][b];
                        }
                    }

                    for (Contig d : QuerySpec.contigs) {
                        for (Feature g : d.featureSet) {
                            if (g.isProteinProduct()) {
                                //System.out.println(d.getName() + g.getShortcut());
                                int Queryindex = QuerySc2Int.get(g.getShortcut());
                                newAlignScore[existingFeatures][Queryindex] = AlignmentTools.quickalign(f.getProteinProduct().getSequence(), g.getProteinProduct().getSequence(), sm, AlignmentTools.Local);
                                newPercentage[existingFeatures][Queryindex] = (double) newAlignScore[existingFeatures][Queryindex] / (double) newSelfAlignScore[existingFeatures];
                            }
                        }
                    }

                    AlignScore = newAlignScore;
                    percentage = newPercentage;
                    selfAlignScore = newSelfAlignScore;
                    ReferenceInt2Sc = newReferenceInt2Sc;
                }
            }
        }
    }

    public void calculate(SubstitutionMatrix sm) {
        System.out.println("Start Genome comparison");

        for (Contig c : ReferenceSpec.contigs) {
            for (Feature f : c.featureSet) {
                if (f.isProteinProduct()) {
                    int Refindex = ReferenceSc2Int.get(f.getShortcut());
                    System.out.println("Genomecomparison: Feature " + Refindex + " of " + selfAlignScore.length);
                    selfAlignScore[Refindex] = AlignmentTools.quickalign(f.getProteinProduct().getSequence(), f.getProteinProduct().getSequence(), sm, AlignmentTools.Local);

                    for (Contig d : QuerySpec.contigs) {
                        for (Feature g : d.featureSet) {
                            if (g.isProteinProduct()) {
                                //System.out.println(d.getName() + g.getShortcut());
                                int Queryindex = QuerySc2Int.get(g.getShortcut());
                                //System.out.println(f.getProteinProduct().getSequence().toString());
                                //System.out.println(g.getProteinProduct().getSequence().toString());
                                AlignScore[Refindex][Queryindex] = AlignmentTools.quickalign(f.getProteinProduct().getSequence(), g.getProteinProduct().getSequence(), sm, AlignmentTools.Local);
                                percentage[Refindex][Queryindex] = (double) AlignScore[Refindex][Queryindex] / (double) selfAlignScore[Refindex];
                            }
                        }
                    }
                }
            }
        }


    }


}
