package com.holberton.realestate.exception;

public class OwnerNotFoundException extends RuntimeException {

    public OwnerNotFoundException(Long id) {
        super("Owner not found: " + id);
    }
}