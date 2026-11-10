package com.holberton.realestate.checks.support;

import java.util.concurrent.atomic.AtomicInteger;

/** The JSON bodies sent by the checks, written as plain text. */
public final class Bodies {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private Bodies() {
    }

    /** A valid property, with a default street and zip code. */
    public static String property(String reference, String city, int surface, int monthlyRent,
            String energyClass) {
        return """
                {"reference": "%s",
                 "address": {"street": "1 Rue des Tests", "zipCode": "75001", "city": "%s"},
                 "surface": %d, "monthlyRent": %d, "energyClass": "%s"}
                """.formatted(reference, city, surface, monthlyRent, energyClass);
    }

    /** A valid property with every field chosen by the check. */
    public static String property(String reference, String street, String zipCode, String city,
            int surface, int monthlyRent, String energyClass) {
        return """
                {"reference": "%s",
                 "address": {"street": "%s", "zipCode": "%s", "city": "%s"},
                 "surface": %d, "monthlyRent": %d, "energyClass": "%s"}
                """.formatted(reference, street, zipCode, city, surface, monthlyRent, energyClass);
    }

    /** A property of a given type, with the extra fields of task 15. */
    public static String typedProperty(String reference, String city, int surface, int monthlyRent,
            String energyClass, String type, String extraFields) {
        return """
                {"reference": "%s",
                 "address": {"street": "1 Rue des Tests", "zipCode": "75001", "city": "%s"},
                 "surface": %d, "monthlyRent": %d, "energyClass": "%s", "type": "%s"%s}
                """.formatted(reference, city, surface, monthlyRent, energyClass, type,
                        extraFields.isEmpty() ? "" : ", " + extraFields);
    }

    public static String owner(String name, String email) {
        return "{\"name\": \"%s\", \"email\": \"%s\"}".formatted(name, email);
    }

    public static String tenant(String name, String email, int monthlyIncome) {
        return "{\"name\": \"%s\", \"email\": \"%s\", \"monthlyIncome\": %d}"
                .formatted(name, email, monthlyIncome);
    }

    public static String lease(String propertyReference, long tenantId, int durationMonths) {
        return "{\"propertyReference\": \"%s\", \"tenantId\": %d, \"durationMonths\": %d}"
                .formatted(propertyReference, tenantId, durationMonths);
    }

    public static String ownerAssignment(Long ownerId) {
        return "{\"ownerId\": " + ownerId + "}";
    }

    /** An email nobody else uses, so that the checks never fight over a unique column. */
    public static String email(String prefix) {
        return prefix + "-" + COUNTER.incrementAndGet() + "-" + System.nanoTime() + "@example.com";
    }
}
