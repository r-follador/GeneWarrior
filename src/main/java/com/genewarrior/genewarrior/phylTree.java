/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.genewarrior.genewarrior;

import java.util.ArrayList;

/**
 * @author rainer
 */
public class phylTree {

    public static ArrayList<String> makeSVG(String newick, int width, int heightPerEntry) throws FormattingException {
        node n;

        try {
            n = new node(newick);
        } catch (FormattingException e) {
            throw e;
        }

        double leaveDescriptionPos = 0.5; //in fraction of the width
        int entries = n.getNrLeaves();
        int height = heightPerEntry * entries + 20;
        double maxLength = n.getMaxLength();
        //System.err.println("maxLength: "+maxLength);


        double factorX = (width * leaveDescriptionPos - 20) / maxLength;

        //System.err.println("factorX: "+factorX);


        ArrayList<String> html = new ArrayList<>();
        html.add("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height + "\">");
        n.drawSVG(html, factorX, heightPerEntry, 10, 10);
        html.add("</svg>");
        return html;
    }

    public static class node {

        String name;
        double dist;
        node subnode1;
        node subnode2;
        boolean isLeave = false;

        public node(String newick) throws FormattingException {
            String restTree;
            if (newick.endsWith(";")) {
                dist = 0;
                restTree = newick.substring(0, newick.length() - 1);
            } else {
                int pos = newick.lastIndexOf(":");
                if (pos < 0 || pos >= newick.length() - 1) {
                    throw new FormattingException("No distance in String: '" + newick + "'");
                }
                restTree = newick.substring(0, pos);
                String distStr = newick.substring(pos + 1);
                try {
                    dist = Double.parseDouble(distStr);
                } catch (NumberFormatException e) {
                    throw new FormattingException("Cannot convert distance in String: '" + newick + "'");
                }
            }
            if (restTree.startsWith("(") && restTree.endsWith(")")) {
                restTree = restTree.substring(1, restTree.length() - 1); //remove the parenthesis
            }
            int sepPos = getSeparation(restTree);
            if (sepPos < 0) {
                isLeave = true;
                name = restTree;
            } else if (sepPos > restTree.length() - 3) {
                throw new FormattingException("Can't parse: '" + restTree + "'");
            } else {
                String firstNode = restTree.substring(0, sepPos);
                String secondNode = restTree.substring(sepPos + 1);

                subnode1 = new node(firstNode);
                subnode2 = new node(secondNode);
            }

        }

        private void drawSVG(ArrayList<String> svg, double factorX, double heightPerEntry, int x, int y) {
            int x0 = x;
            int y0 = y + (int) ((getNrLeaves() * heightPerEntry) / 2);

            int x1 = x0 + (int) (subnode1.dist * factorX);
            int y1 = y + (int) ((subnode1.getNrLeaves() * heightPerEntry) / 2);

            int x2 = x0 + (int) (subnode2.dist * factorX);
            int y2 = y + (int) (subnode1.getNrLeaves() * heightPerEntry) + (int) ((subnode2.getNrLeaves() * heightPerEntry) / 2);


            //subnode1
            //svg.add("<path d=\"M" + x0 + "," + y0 + " Q" + x0 + "," + y1 + " " + x1 + "," + y1 + "\" style=\"stroke: #006666; fill:none;\"/>");
            svg.add("<polyline points=\"" + x0 + "," + y0 + " " + x0 + "," + y1 + " " + x1 + "," + y1 + "\" style=\"stroke: #006666; fill:none;\"/>");
            //subnode2
            //svg.add("<path d=\"M" + x0 + "," + y0 + " Q" + x0 + "," + y2 + " " + x2 + "," + y2 + "\" style=\"stroke: #006666; fill:none;\"/>");
            svg.add("<polyline points=\"" + x0 + "," + y0 + " " + x0 + "," + y2 + " " + x2 + "," + y2 + "\" style=\"stroke: #006666; fill:none;\"/>");

            if (subnode1.isLeave) {
                svg.add("<circle cx=\"" + x1 + "\" cy=\"" + y1 + "\" r=\"5\" style=\"stroke:#aaaacc; fill:#aaaacc\"/>");
                svg.add("<text x=\"" + (x1 + 10) + "\"  y=\"" + (y1 + 5) + "\" font-size=\"12\" font-family=\"'Lora', Helvetica,Arial,sans-serif;\">" + subnode1.name + "</text>");
            } else {
                subnode1.drawSVG(svg, factorX, heightPerEntry, x1, y);
            }

            if (subnode2.isLeave) {
                svg.add("<circle cx=\"" + x2 + "\" cy=\"" + y2 + "\" r=\"5\" style=\"stroke:#aaaacc; fill:#aaaacc\"/>");
                svg.add("<text x=\"" + (x2 + 10) + "\"  y=\"" + (y2 + 5) + "\" font-size=\"12\" font-family=\"'Lora', Helvetica,Arial,sans-serif;\">" + subnode2.name + "</text>");
            } else {
                subnode2.drawSVG(svg, factorX, heightPerEntry, x2, y + (int) (subnode1.getNrLeaves() * heightPerEntry));
            }

            svg.add("<circle cx=\"" + x0 + "\" cy=\"" + y0 + "\" r=\"3\" style=\"stroke:#aaaacc; fill:#ffffff; stroke-width: 2px\"/>");

        }

        public void printNode() {
            printNode(0);
        }

        private void printNode(int depth) {
            String indent = "";
            for (int i = 0; i < depth; i++) {
                indent += "-";
            }
            if (isLeave) {
                System.out.println(indent + "dist: " + dist + " " + name);
            } else {
                System.out.println(indent + "dist: " + dist);
                subnode1.printNode(depth + 1);
                subnode2.printNode(depth + 1);
            }
        }

        public double getMaxLength() {
            if (isLeave) {
                return dist;
            } else {
                double s1 = subnode1.getMaxLength();
                double s2 = subnode2.getMaxLength();
                if (s1 > s2) {
                    return s1 + dist;
                } else {
                    return s2 + dist;
                }
            }
        }

        public int getNrLeaves() {
            if (isLeave) {
                return 1;
            } else {
                return subnode1.getNrLeaves() + subnode2.getNrLeaves();
            }
        }
    }

    public static class FormattingException extends Exception {

        public String descr;

        public FormattingException(String descr) {
            this.descr = descr;
        }
    }

    private static int getSeparation(String text) throws FormattingException {
        int firstParen = text.indexOf('(');
        int closingParen;
        if (firstParen < 0) {
            closingParen = -1;
            //return text.indexOf(',');
        } else {
            closingParen = findClosingParen(text.toCharArray(), firstParen);
        }

        int hypothesizedPos;
        if (closingParen < 0) {
            hypothesizedPos = text.indexOf(',');
        } else {
            hypothesizedPos = text.indexOf(',');

            if (hypothesizedPos > firstParen && hypothesizedPos < closingParen) {//comma is in parenthesis
                hypothesizedPos = text.indexOf(',', closingParen);
            }
        }
        if (hypothesizedPos < 0) {
            return -1;
        }
        //check hypothesizedPos
        String firstNode = text.substring(0, hypothesizedPos);
        String secondNode = text.substring(hypothesizedPos + 1);

        if ((firstNode.endsWith(";") || firstNode.matches(".+:\\d+.*")) && (secondNode.endsWith(";") || secondNode.matches(".+:\\d+.*"))) {
            return hypothesizedPos;
        }
        return -1;
    }

    private static String extractInnerParen(String text) throws FormattingException {
        int firstParen = text.indexOf('(');
        if (firstParen < 0) {
            return null;
        }
        int closingParen;
        closingParen = findClosingParen(text.toCharArray(), firstParen);

        if (closingParen < 0) {
            return null;
        }
        return text.substring(firstParen + 1, closingParen);
    }

    private static int findClosingParen(char[] text, int openPos) throws FormattingException {
        int length = text.length;
        int closePos = openPos;
        int counter = 1;
        while (counter > 0) {
            if (closePos + 1 == length) //not found
            {
                throw new FormattingException("No closing parenthesis in String: '" + text.toString() + "'");
            }
            char c = text[++closePos];
            if (c == '(') {
                counter++;
            } else if (c == ')') {
                counter--;
            }
        }
        return closePos;
    }
}
