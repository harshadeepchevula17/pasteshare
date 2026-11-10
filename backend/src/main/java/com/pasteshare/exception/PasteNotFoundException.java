package com.pasteshare.exception;

public class PasteNotFoundException extends RuntimeException {

    public PasteNotFoundException(String id) {
        super("Paste " + id + " was not found.");
    }
}
