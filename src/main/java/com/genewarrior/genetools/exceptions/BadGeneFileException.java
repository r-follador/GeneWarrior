package com.genewarrior.genetools.exceptions;

public class BadGeneFileException extends Exception {
    public final static int NOT_A_GENEWARRIOR_FILE = 1;
    public final static int CAST_ERROR = 2;
    public final static int IO_ERROR = 3;

    public int error_number = 0;

    public BadGeneFileException(int error) {
        error_number = error;
		/*switch (error)
		case NOT_A_GENEWARRIOR_FILE:
		{
			
		}
		case CAST_ERROR
		{
		
		}*/
    }
}
