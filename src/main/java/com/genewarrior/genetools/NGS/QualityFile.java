/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.genewarrior.genetools.NGS;

import com.genewarrior.genetools.exceptions.QualitySequenceFileException;
import com.genewarrior.genetools.sequenceHandling.SequenceDNA;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author corona
 */
public class QualityFile implements Iterable<QualitySequenceDNA> {

    public QualityFile(File FASTQ) throws FileNotFoundException {
        isFASTQ = true;
        fastqReader = new BufferedReader(new FileReader(FASTQ));
        iterator = new QualityFileIterator();
    }

    public QualityFile(File FASTA, File QUAL) throws FileNotFoundException {
        isFASTQ = false;
        fastaReader = new BufferedReader(new FileReader(FASTA));
        qualReader = new BufferedReader(new FileReader(QUAL));
        try {
            lastLineFASTA = fastaReader.readLine();
            lastLineQUAL = qualReader.readLine();
        } catch (IOException ex) {
            lastLineFASTA = null;
            lastLineQUAL = null;
        }
        iterator = new QualityFileIterator();
    }


    boolean isFASTQ;
    boolean noNext = false;
    BufferedReader fastqReader;
    BufferedReader fastaReader;
    BufferedReader qualReader;

    String lastLineFASTA = "";
    String lastLineQUAL = "";

    QualitySequenceDNA currentSeq;

    boolean nextCalled = true;

    QualityFileIterator iterator;

    public void close() throws IOException {
        if (isFASTQ) {
            fastqReader.close();
        } else {
            fastaReader.close();
            qualReader.close();
        }
    }

    public Iterator<QualitySequenceDNA> iterator() {
        return iterator;
    }

    public class QualityFileIterator implements Iterator<QualitySequenceDNA> {
        private void loadNext() throws IOException, QualitySequenceFileException {
            if (isFASTQ) {
                String name = fastqReader.readLine();
                if (name == null) {
                    currentSeq = null;
                    noNext = true;
                    return;
                }
                if (!name.startsWith("%")) {
                    throw new QualitySequenceFileException(QualitySequenceFileException.MALFORMATTED_INPUT_FILE);
                }
                name = name.trim().substring(1);
                String seq = fastqReader.readLine();
                if (seq == null) {
                    throw new QualitySequenceFileException(QualitySequenceFileException.MALFORMATTED_INPUT_FILE);
                }
                String buffer = fastqReader.readLine();
                if (buffer == null) {
                    throw new QualitySequenceFileException(QualitySequenceFileException.MALFORMATTED_INPUT_FILE);
                }

                String qualities = fastqReader.readLine();
                if (qualities == null) {
                    throw new QualitySequenceFileException(QualitySequenceFileException.MALFORMATTED_INPUT_FILE);
                }

                byte[] q = qualities.trim().getBytes();
                for (int i = 0; i < q.length; i++) {
                    q[i] = (byte) (q[i] - 33);
                }

                currentSeq = new QualitySequenceDNA(name, new SequenceDNA(seq.trim(), false), q);
            } else //FASTA
            {
                if (lastLineQUAL == null || lastLineFASTA == null || !lastLineQUAL.startsWith(">") || !lastLineFASTA.startsWith(">")) {
                    throw new QualitySequenceFileException(QualitySequenceFileException.MALFORMATTED_INPUT_FILE);
                }
                if (!lastLineQUAL.equals(lastLineFASTA)) {
                    throw new QualitySequenceFileException(QualitySequenceFileException.MALFORMATTED_INPUT_FILE);
                }

                String name = lastLineQUAL.trim().substring(1);

                String seq = "";
                String qual = "";
                while (true) //read FASTA
                {
                    String outputFASTA = fastaReader.readLine();
                    if (outputFASTA == null) {
                        noNext = true;
                        break;
                    }
                    if (outputFASTA.startsWith(">")) {
                        lastLineFASTA = outputFASTA;
                        break;
                    }
                    seq += outputFASTA.trim();
                }

                while (true) //read Qual
                {
                    String outputQUAL = qualReader.readLine();
                    if (outputQUAL == null) {
                        noNext = true;
                        break;
                    }
                    if (outputQUAL.startsWith(">")) {
                        lastLineQUAL = outputQUAL;
                        break;
                    }
                    qual += outputQUAL.trim() + " ";
                }

                String[] quals = qual.split("\\s");
                byte[] q = new byte[quals.length];

                for (int i = 0; i < q.length; i++) {
                    q[i] = Byte.decode(quals[i]);
                }

                currentSeq = new QualitySequenceDNA(name, new SequenceDNA(seq.trim(), false), q);
            }
        }

        public boolean hasNext() {
            if (noNext && nextCalled)
                return false;
            if (nextCalled) {
                try {
                    loadNext();
                } catch (IOException ex) {
                    return false;
                } catch (QualitySequenceFileException ex) {
                    return false;
                }
                nextCalled = false;
                return true;
            } else {
                return true;
            }
        }

        public QualitySequenceDNA next() {
            if (!hasNext())
                throw new NoSuchElementException();
            nextCalled = true;
            return currentSeq;
        }

        public void remove() {
            throw new UnsupportedOperationException("Cannot remove. Read only.");
        }
    }
}
