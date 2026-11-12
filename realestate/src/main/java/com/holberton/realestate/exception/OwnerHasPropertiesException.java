package com.holberton.realestate.exception;

public class OwnerHasPropertiesException extends RuntimeException {

    public OwnerHasPropertiesException(Long id) {
        super("Owner " + id + " still owns at least one property");
    }
}