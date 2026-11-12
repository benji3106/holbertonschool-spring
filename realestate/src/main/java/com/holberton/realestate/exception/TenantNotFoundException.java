package com.holberton.realestate.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(Long id) {
        super("Tenant not found: " + id);
    }
}