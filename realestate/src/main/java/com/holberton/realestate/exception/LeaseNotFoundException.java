package com.holberton.realestate.exception;

public class LeaseNotFoundException extends RuntimeException {

    public LeaseNotFoundException(Long id) {
        super("Lease not found: " + id);
    }
}