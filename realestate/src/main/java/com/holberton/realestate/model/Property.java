package com.holberton.realestate.model;

public class Property {

    private final String reference;
    private Address address;
    private int surface;
    private int monthlyRent;
    private EnergyClass energyClass;

    public Property(String reference, Address address, int surface, int monthlyRent,
                    EnergyClass energyClass) {
        this.reference = reference;
        this.address = address;
        this.surface = surface;
        this.monthlyRent = monthlyRent;
        this.energyClass = energyClass;
    }

    public String getReference() {
        return reference;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getSurface() {
        return surface;
    }

    public void setSurface(int surface) {
        this.surface = surface;
    }

    public int getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(int monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public EnergyClass getEnergyClass() {
        return energyClass;
    }

    public void setEnergyClass(EnergyClass energyClass) {
        this.energyClass = energyClass;
    }
}