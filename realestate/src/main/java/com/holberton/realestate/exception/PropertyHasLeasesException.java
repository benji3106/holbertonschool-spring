package com.holberton.realestate.exception;

public class PropertyHasLeasesException extends RuntimeException {

    public PropertyHasLeasesException(String reference) {
        super("Property " + reference + " has leases and cannot be deleted");
    }
}