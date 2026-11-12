package com.holberton.realestate.dto;

import com.holberton.realestate.model.Owner;
import com.holberton.realestate.model.Property;

public final class PropertyMapper {

    private static final int MONTHS_PER_YEAR = 12;

    private PropertyMapper() {
        // utility class: no instance
    }

    public static PropertyResponse toResponse(Property property) {
        Owner owner = property.getOwner();
        Long ownerId = owner == null ? null : owner.getId();
        return new PropertyResponse(property.getReference(), property.getAddress(),
                property.getSurface(), property.getMonthlyRent(), property.getEnergyClass(),
                property.getMonthlyRent() * MONTHS_PER_YEAR, ownerId, property.isRented());
    }

    public static Property toEntity(PropertyRequest request) {
        return new Property(request.reference(), request.address(), request.surface(),
                request.monthlyRent(), request.energyClass());
    }
}