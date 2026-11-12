package com.holberton.realestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Embeddable
public record Address(
        @NotBlank @Column(nullable = false) String street,
        @NotNull @Pattern(regexp = "\\d{5}") @Column(nullable = false) String zipCode,
        @NotBlank @Column(nullable = false) String city) {
}