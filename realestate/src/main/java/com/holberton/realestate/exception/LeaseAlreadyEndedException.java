package com.holberton.realestate.exception;

public class LeaseAlreadyEndedException extends RuntimeException {

    public LeaseAlreadyEndedException(Long id) {
        super("Lease " + id + " is already ended");
    }
}