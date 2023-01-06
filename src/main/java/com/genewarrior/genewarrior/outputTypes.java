package com.genewarrior.genewarrior;

import java.util.ArrayList;

public class outputTypes {

    public enum OutputType {

        error, dna, aa, text, translateWindow, alignmentDNA, alignmentProt, shareInfo, primer3
    }

    public static class mark {

        public mark(int type, int start, int stop, String name) {
            this.type = type;
            this.start = start;
            this.stop = stop;
            this.name = name;
        }

        String name;
        int type;
        int start;
        int stop;
    }

    public static class output {

        public output(OutputType type) {
            this.type = type;
        }

        OutputType type;
    }

    public static class alignmentDNA extends output {

        public alignmentDNA(String name, ArrayList<Dna> seqs, String pos, String info, String matches) {
            super(OutputType.alignmentDNA);
            this.seqs = seqs;
            this.name = name;
            this.pos = pos;
            this.matches = matches;
            this.info = info;
            this.mark = new ArrayList<>();
        }

        public alignmentDNA(String name, ArrayList<Dna> seqs, String pos, String info, String matches, ArrayList<mark> mark) {
            super(OutputType.alignmentDNA);
            this.seqs = seqs;
            this.name = name;
            this.pos = pos;
            this.matches = matches;
            this.info = info;
            this.mark = mark;
        }

        String info;
        String pos;
        String name;
        String matches;
        ArrayList<Dna> seqs;
        ArrayList<mark> mark;
    }

    public static class alignmentProt extends output {

        public alignmentProt(String name, ArrayList<Aa> seqs, String pos, String info, String matches) {
            super(OutputType.alignmentProt);
            this.seqs = seqs;
            this.name = name;
            this.pos = pos;
            this.matches = matches;
            this.info = info;
            this.mark = new ArrayList<>();
        }

        public alignmentProt(String name, ArrayList<Aa> seqs, String pos, String info, String matches, ArrayList<mark> mark) {
            super(OutputType.alignmentProt);
            this.seqs = seqs;
            this.name = name;
            this.pos = pos;
            this.matches = matches;
            this.info = info;
            this.mark = mark;
        }

        ArrayList<Aa> seqs;
        String info;
        String name;
        String pos;
        String matches;
        ArrayList<mark> mark;
    }

    public static class error extends output {

        public error(String error) {
            super(OutputType.error);
            this.error = error;
        }

        String error;
    }

    public static class CustomText extends output {

        public CustomText(String title, String info, String html) {
            super(OutputType.text);
            this.name = title;
            this.info = info;
            this.html = html;
        }

        public CustomText(String title, String info, String html, String pos) {
            super(OutputType.text);
            this.name = title;
            this.info = info;
            this.html = html;
            this.pos = pos;
        }

        String pos = "-1";
        String name;
        String info;
        String html;
    }

    public static class Dna extends output {

        public Dna(String name, String sequence) {
            super(OutputType.dna);
            this.name = name;
            this.sequence = sequence;
            this.mark = new ArrayList<>();
            setInfo();
        }

        public Dna(String name, String sequence, ArrayList<mark> mark) {
            super(OutputType.dna);
            this.name = name;
            this.sequence = sequence;
            this.mark = mark;
            setInfo();
        }

        public Dna(String name, String sequence, ArrayList<mark> mark, String pos) {
            super(OutputType.dna);
            this.name = name;
            this.sequence = sequence;
            this.mark = mark;
            this.pos = pos;
            setInfo();
        }

        public Dna(String name, String sequence, String pos) {
            super(OutputType.dna);
            this.name = name;
            this.sequence = sequence;
            this.mark = new ArrayList<>();
            this.pos = pos;
            setInfo();
        }

        private void setInfo() {
            info = "DNA: " + sequence.length() + " bp";
        }

        String pos = "-1";
        String name;
        String sequence;
        ArrayList<mark> mark;
        String info;
    }

    public static class ShareInfo extends output {

        String dbKey;

        public ShareInfo(String dbKey) {
            super(OutputType.shareInfo);
            this.dbKey = dbKey;
        }
    }

    public static class TranslateWindowClass extends output {

        String seq;

        public TranslateWindowClass(String seq, String f1, String f2, String f3, String tf1, String tf2, String tf3) {
            super(OutputType.translateWindow);
            this.seq = seq;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.tf1 = tf1;
            this.tf2 = tf2;
            this.tf3 = tf3;
        }

        String f1;
        String f2;
        String f3;
        String tf1;
        String tf2;
        String tf3;
    }

    public static class Aa extends output {

        public Aa(String name, String sequence) {
            super(OutputType.aa);
            this.name = name;
            this.sequence = sequence;
            this.mark = new ArrayList<>();
            setInfo();
        }

        public Aa(String name, String sequence, ArrayList<mark> mark) {
            super(OutputType.aa);
            this.name = name;
            this.sequence = sequence;
            this.mark = mark;
            setInfo();
        }

        public Aa(String name, String sequence, ArrayList<mark> mark, String pos) {
            super(OutputType.aa);
            this.name = name;
            this.sequence = sequence;
            this.mark = mark;
            this.pos = pos;
            setInfo();
        }

        public Aa(String name, String sequence, String pos) {
            super(OutputType.aa);
            this.name = name;
            this.sequence = sequence;
            this.mark = new ArrayList<>();
            this.pos = pos;
            setInfo();
        }

        private void setInfo() {
            int length = sequence.length();
            int stops = length - sequence.replace("*", "").length();
            info = "Protein: " + (length - stops) + " Aa" + (stops > 0 ? "; " + stops + " stop" : "");
        }

        String pos = "-1";
        String name;
        String sequence;
        ArrayList<mark> mark;
        String info;
    }

    public static class Primer3 extends output {

        ArrayList<PrimerPair> primerPairs = new ArrayList<>();
        String explanation = "";
        String error = "";

        public Primer3() {
            super(OutputType.primer3);
        }


        public static class PrimerPair {

            String leftSequence;
            String rightSequence;
            String internalSequence;
            int leftPosition;
            int rightPosition;
            int internalPosition;
            float leftTm;
            float rightTm;
            float internalTm;
            int leftGC;
            int rightGC;
            int internalGC;
            int productSize;
            boolean hasInternal = false;

            public PrimerPair(String leftSequence, String rightSequence, String internalSequence, int leftPosition, int rightPosition, int internalPosition, float leftTm, float rightTm, float internalTm, int leftGC, int rightGC, int internalGC, int productSize) {
                this.leftSequence = leftSequence;
                this.rightSequence = rightSequence;
                this.internalSequence = internalSequence;
                this.leftPosition = leftPosition;
                this.rightPosition = rightPosition;
                this.internalPosition = internalPosition;
                this.leftTm = leftTm;
                this.rightTm = rightTm;
                this.internalTm = internalTm;
                this.rightGC = rightGC;
                this.leftGC = leftGC;
                this.internalGC = internalGC;
                this.productSize = productSize;
                hasInternal = true;
            }

            public PrimerPair(String leftSequence, String rightSequence, int leftPosition, int rightPosition, float leftTm, float rightTm, int leftGC, int rightGC, int productSize) {
                this.leftSequence = leftSequence;
                this.rightSequence = rightSequence;
                this.leftPosition = leftPosition;
                this.rightPosition = rightPosition;
                this.leftTm = leftTm;
                this.rightTm = rightTm;
                this.productSize = productSize;
                this.rightGC = rightGC;
                this.leftGC = leftGC;
                hasInternal = false;
            }
        }
    }
}
