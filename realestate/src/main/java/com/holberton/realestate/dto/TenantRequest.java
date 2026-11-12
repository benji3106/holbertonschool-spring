package com.holberton.realestate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TenantRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @PositiveOrZero int monthlyIncome) {
}