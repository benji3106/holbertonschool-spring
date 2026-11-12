package com.holberton.realestate.dto;

import com.holberton.realestate.model.Lease;

public final class LeaseMapper {

    private LeaseMapper() {
        // utility class: no instance
    }

    public static LeaseResponse toResponse(Lease lease) {
        return new LeaseResponse(lease.getId(), lease.getProperty().getReference(),
                lease.getTenant().getId(), lease.getDurationMonths(), lease.getMonthlyRent(),
                lease.getTotalRent(), lease.getDeposit(), lease.isActive());
    }
}