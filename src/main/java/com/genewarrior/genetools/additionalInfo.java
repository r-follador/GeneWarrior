package com.genewarrior.genetools;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * additionalInfo for any feature; consists of headers, titles, and texts
 * use <code>add(String header, String title, String text)</add> to add new entries
 *
 */
public class additionalInfo implements Cloneable, Serializable {

    private static final long serialVersionUID = -7899727285987903539L;
    ArrayList<String> headers = new ArrayList<String>();
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> texts = new ArrayList<String>();

    /**
     * Returns the first entry which corresponds to header and title
     *
     * @param header
     * @param title
     * @return String[0] header, String[1] title, String[2] text
     */
    public String[] getFirstEntry(String header, String title) {
        int size = getSize();
        String[] line = new String[3];
        for (int i = 0; i < size; i++) {
            if (headers.get(i).trim().equalsIgnoreCase(header))
                if (titles.get(i).trim().equalsIgnoreCase(title)) {
                    line[0] = headers.get(i);
                    line[1] = titles.get(i);
                    line[2] = texts.get(i);
                    return line;
                }
        }
        return null;
    }

    public int getFirstEntryIndex(String header, String title) {
        int size = getSize();
        for (int i = 0; i < size; i++) {
            if (headers.get(i).trim().equalsIgnoreCase(header))
                if (titles.get(i).trim().equalsIgnoreCase(title)) {
                    return i;
                }
        }
        return -1;
    }


    /**
     * Adds a new entry to additionalInfo
     *
     * @param header Type of Information
     * @param title  Title of Information
     * @param text   Text of Information
     * @return
     */
    public boolean add(String header, String title, String text) {
        headers.add(header);
        titles.add(title);
        texts.add(text);
        return true;
    }

    /**
     * Deletes all entries
     */
    public void deleteAll() {
        headers = new ArrayList<String>();
        titles = new ArrayList<String>();
        texts = new ArrayList<String>();
    }

    public void deleteIndex(int i) {
        if (i < 0 || i > getSize() - 1)
            return;
        headers.remove(i);
        titles.remove(i);
        texts.remove(i);
    }

    /**
     * @return number of entries
     */
    public int getSize() {
        return headers.size();
    }

    /**
     * @return String array containing entries. String[x][y]: x number of entry; y 0: headers; 1: titles; 2: texts
     */
    public String[][] getString() {
        String[][] outputList = new String[headers.size()][3];

        for (int i = 0; i < headers.size(); i++) {
            outputList[i][0] = headers.get(i);
            outputList[i][1] = titles.get(i);
            outputList[i][2] = texts.get(i);
        }

        return outputList;

    }

    /**
     * Deep clone of <code>additionalInfo</code>
     */
    @Override
    public Object clone() {
        try {
            additionalInfo cloned = (additionalInfo) super.clone();
            cloned.headers = (ArrayList<String>) headers.clone();
            cloned.titles = (ArrayList<String>) titles.clone();
            cloned.texts = (ArrayList<String>) texts.clone();

            return cloned;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
