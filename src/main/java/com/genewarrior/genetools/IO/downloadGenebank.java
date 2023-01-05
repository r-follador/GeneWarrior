/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.genewarrior.genetools.IO;

import com.genewarrior.genetools.Species;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author btserver
 */
public class downloadGenebank {

    public static Species downloadGenebank(int genomeid) {
        File temp = null;
        try {
            URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=genome&id=" + genomeid + "&rettype=gp&retmode=text");
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            String inputLine;
            temp = new File("temp");
            temp.createNewFile();
            FileWriter schreiber = new FileWriter(temp);
            BufferedWriter bschreiber = new BufferedWriter(schreiber);


            while ((inputLine = in.readLine()) != null)
                if (inputLine.equalsIgnoreCase("nothing has been found")) {
                    System.out.println("Not a valid genomeid: " + genomeid);
                    in.close();
                    bschreiber.close();
                    schreiber.close();
                    temp.delete();
                    return null;
                }
            bschreiber.write(inputLine + "\n");
            in.close();
            bschreiber.close();
            schreiber.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            genbankRead dgb = new genbankRead(temp);
            return dgb.getSpecies();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}
