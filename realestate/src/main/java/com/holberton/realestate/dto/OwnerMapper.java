package com.holberton.realestate.dto;

import com.holberton.realestate.model.Owner;

public final class OwnerMapper {

    private OwnerMapper() {
        // utility class: no instance
    }

    public static OwnerResponse toResponse(Owner owner) {
        return new OwnerResponse(owner.getId(), owner.getName(), owner.getEmail());
    }

    public static Owner toEntity(OwnerRequest request) {
        return new Owner(request.name(), request.email());
    }
}