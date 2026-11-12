package com.holberton.realestate.dto;

import jakarta.validation.constraints.NotNull;

public record OwnerAssignmentRequest(@NotNull Long ownerId) {
}