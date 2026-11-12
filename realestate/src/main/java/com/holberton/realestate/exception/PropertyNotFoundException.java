package com.holberton.realestate.exception;

public class PropertyNotFoundException extends RuntimeException {

    public PropertyNotFoundException(String reference) {
        super("Property not found: " + reference);
    }
}