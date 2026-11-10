package com.holberton.realestate.model;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String street, String zipCode, String city) {
}