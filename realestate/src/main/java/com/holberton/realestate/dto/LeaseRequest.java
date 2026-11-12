package com.holberton.realestate.dto;

import com.holberton.realestate.model.Lease;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LeaseRequest(
        @NotBlank String propertyReference,
        @NotNull Long tenantId,
        @Min(Lease.MIN_DURATION_MONTHS) @Max(Lease.MAX_DURATION_MONTHS) int durationMonths) {
}