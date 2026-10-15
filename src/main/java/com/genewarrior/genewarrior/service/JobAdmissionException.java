package com.genewarrior.genewarrior.service;

public class JobAdmissionException extends RuntimeException {
    private final int status;

    public JobAdmissionException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
