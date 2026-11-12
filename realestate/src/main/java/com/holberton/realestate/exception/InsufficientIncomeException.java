package com.holberton.realestate.exception;

public class InsufficientIncomeException extends RuntimeException {

    public InsufficientIncomeException(int income, int requiredIncome) {
        super("The income of the tenant (" + income + ") is lower than the required income ("
                + requiredIncome + ")");
    }
}