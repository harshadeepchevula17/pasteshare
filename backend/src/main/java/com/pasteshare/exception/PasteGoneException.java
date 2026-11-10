package com.pasteshare.exception;

public class PasteGoneException extends RuntimeException {

    public PasteGoneException() {
        super("Paste has expired or reached its view limit.");
    }
}
