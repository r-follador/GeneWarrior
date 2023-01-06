package com.genewarrior.genetools.exceptions;

public class QualitySequenceFileException extends Exception {
    public final static int MALFORMATTED_INPUT_FILE = 1;
    public final static int CAST_ERROR = 2;
    public final static int IO_ERROR = 3;

    public int error_number = 0;

    public QualitySequenceFileException(int error) {
        error_number = error;
    }
}
