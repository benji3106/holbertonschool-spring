package com.holberton.realestate.dto;

import com.holberton.realestate.model.Tenant;

public final class TenantMapper {

    private TenantMapper() {
        // utility class: no instance
    }

    public static TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getEmail(),
                tenant.getMonthlyIncome());
    }

    public static Tenant toEntity(TenantRequest request) {
        return new Tenant(request.name(), request.email(), request.monthlyIncome());
    }
}