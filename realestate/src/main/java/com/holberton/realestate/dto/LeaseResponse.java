package com.holberton.realestate.dto;

public record LeaseResponse(Long id, String propertyReference, Long tenantId, int durationMonths,
                            int monthlyRent, int totalRent, int deposit, boolean active) {
}