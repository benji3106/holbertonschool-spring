package com.holberton.realestate.exception;

public class PropertyUnavailableException extends RuntimeException {

    public PropertyUnavailableException(String reference) {
        super("Property " + reference + " already has an active lease");
    }
}