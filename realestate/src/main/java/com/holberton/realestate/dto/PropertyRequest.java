package com.holberton.realestate.dto;

import com.holberton.realestate.model.Address;
import com.holberton.realestate.model.EnergyClass;

public record PropertyRequest(String reference, Address address, int surface,
                              int monthlyRent, EnergyClass energyClass) {
}