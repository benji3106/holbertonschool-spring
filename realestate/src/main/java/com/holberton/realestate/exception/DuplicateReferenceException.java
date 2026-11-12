package com.holberton.realestate.exception;

public class DuplicateReferenceException extends RuntimeException {

    public DuplicateReferenceException(String reference) {
        super("A property already exists with the reference: " + reference);
    }
}