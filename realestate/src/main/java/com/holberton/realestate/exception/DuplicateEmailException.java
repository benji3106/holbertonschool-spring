package com.holberton.realestate.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("This email is already used: " + email);
    }
}